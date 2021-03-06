import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.Socket;
import java.util.Date;

import twamp.twamp;

public class client
{	
	static Boolean URG = false, ACK = false, PSH = false, RST = false, SYN = true, FIN = false ;
	static int src_port = 862, dest_port = 862, seq = twamp.random.nextInt(Short.MAX_VALUE), base_seq, base_ack, ack = 0, HLEN = 5, win_size = 0, checksum = 0, urg_ptr = 0, temp, test_seq = 0, packets_sent, packets_lost = 0, glv ;
	static long OFFSET = 2208988800L, t1_int, t1_frac, t2_int, t2_frac, temp3, start_int, start_frac, end_int, end_frac, timeout_int = 0, timeout_frac = 0, max_rtt = 0, min_rtt = Integer.MAX_VALUE, total_rtt = 0, now ;
	
	static byte [] client_test = new byte[41];
	static byte [] server_test_packet = new byte[client_test.length+8];
	
	static byte [] header = {0x00, 0x00,				// source port
					  		 0x00, 0x00,				// dest port
					  		 0x00, 0x00, 0x00, 0x00,	// seq no.
					  		 0x00, 0x00, 0x00, 0x00,	// ack no.
					  		 0x50,						// first nibble is HLEN, second nibble is 4/6 reserved bits which are all 0
					  		 0x02,						// first nibble lies between 0 and 3 based on URG and ACK, second nibble depends on PSH, RST, SYN and FIN
					  		 0x00, 0x00,				// window size
					  		 0x00, 0x00,				// checksum
					  		 0x00, 0x00};				// urgent pointer

	client(String ip, int port)
	{
		start(ip, port);
	}
	
	static void header()
	{
		twamp.bytearrmod(header, seq, 4, 7);
		twamp.bytearrmod(header, ack, 8, 11);
		header[12] = (byte)(HLEN << 2);
		if(URG)
			header[13] |= 0x20 ;
		else
			header[13] &= ~0x20 ;
		if(ACK)
			header[13] |= 0x10 ;
		else
			header[13] &= ~0x10 ;
		if(PSH)
			header[13] |= 0x08 ;
		else
			header[13] &= ~0x08 ;
		if(RST)
			header[13] |= 0x04 ;
		else
			header[13] &= ~0x04 ;
		if(SYN)
			header[13] |= 0x02 ;
		else
			header[13] &= ~0x02 ;
		if(FIN)
			header[13] |= 0x01 ;
		else
			header[13] &= ~0x01 ;
		for(int x = 0; x < 20; x += 2)
		{
			if(x == 16)
				continue ;
			else
				checksum += (65535-twamp.bytearr_to_int(header, x, x+1));
		}
		checksum = (~checksum) & 0xffff ;
		twamp.bytearrmod(header, checksum, 14, 17);
		header[14] = 0 ;
		header[15] = 0 ;
	}
	
	static void receive()
	{
		src_port = twamp.bytearr_to_int(header, 0,1);
		dest_port = twamp.bytearr_to_int(header, 2,3);
		seq = twamp.bytearr_to_int(header, 4,7);
		ack = twamp.bytearr_to_int(header, 8,11);
		HLEN = (int)(header[12]>>2);
		if((header[13]&0x20)>>5 == 1)
			URG = true ;
		else
			URG = false ;
		if((header[13]&0x10)>>4 == 1)
			ACK = true ;
		else
			ACK = false ;
		if((header[13]&0x08)>>3 == 1)
			PSH = true ;
		else
			PSH = false ;
		if((header[13]&0x04)>>2 == 1)
			RST = true ;
		else
			RST = false ;
		if((header[13]&0x02)>>1 == 1)
			SYN = true ;
		else
			SYN = false ;
		if((header[13]&0x01) == 1)
			FIN = true ;
		else
			FIN = false ;
		win_size = twamp.bytearr_to_int(header, 14,15);
		checksum = twamp.bytearr_to_int(header, 16,17);
		urg_ptr = twamp.bytearr_to_int(header, 18,19);
	}
	
	static int calculate_checksum()
	{
		int cs = 0 ;
		for(int x = 0; x < 20; x += 2)
		{
			if(x == 16)
				continue ;
			else
				cs += (65535-twamp.bytearr_to_int(header, x, x+1));
		}
		cs = (~cs) & 0xffff ;
		return cs ;
	}
	
	static void display()
	{
		System.out.println("Source port : " + src_port);
		System.out.println("Destination port : " + dest_port);
		System.out.println("Sequence number : " + seq + " (Relative sequence number : " + (seq-base_seq) + ")");
		System.out.println("Acknowledgement number : " + ack + " (Relative acknowledgement number : " + (ack-base_ack) + ")");
		System.out.println("TCP Header length : " + header[12] + " bytes.");
		System.out.print("URG = " + ((header[13]&0x20)>>5) + " ");
		System.out.print("ACK = " + ((header[13]&0x10)>>4) + " ");
		System.out.print("PSH = " + ((header[13]&0x08)>>3) + " ");
		System.out.print("RST = " + ((header[13]&0x04)>>2) + " ");
		System.out.print("SYN = " + ((header[13]&0x02)>>1) + " ");
		System.out.println("FIN = " + ((header[13]&0x01)) + " ");
		System.out.println("Window size : " + win_size);
		System.out.println("Checksum : " + checksum + " Calculated checksum : " + calculate_checksum());
		System.out.println("Urgent pointer : " + urg_ptr);
		if(checksum == calculate_checksum())
			System.out.println("Checksums match.");
		else
			System.out.println("Checksums do not match.");
	}
	
	static void start(String i, int p)
	{
		twamp.bytearrmod(header, (short)src_port, 0, 1);
		twamp.bytearrmod(header, (short)dest_port, 2, 3);
		try
		{
			twamp.socket = new Socket(i, p);
			System.out.println("Server is online.\n\n");
			twamp.in = new DataInputStream(twamp.socket.getInputStream());
			twamp.out = new DataOutputStream(twamp.socket.getOutputStream());
			header();
			System.out.println("Request placed in server queue. Wait for queue to free up.\n\n");
			twamp.out.write(header);
			while(true)
			{
				twamp.in.read(header);
				System.out.println("Server is now free to process request from this client.\n\n");
				if((header[13]&0x01) != 1)
				{
					receive();
					twamp.pause(100);
					display();
					System.out.println("\n\n");
					if(SYN && ACK)
					{
						base_seq = ack-1 ;
						base_ack = seq ;
						temp = seq ;
						seq = ack ;
						ack = temp ;
						ack++ ;
						SYN = false ;
						header[13] &= ~0x02 ;
						twamp.bytearrmod(header, seq, 4, 7);
						twamp.bytearrmod(header, ack, 8, 11);
						checksum = calculate_checksum();
						twamp.bytearrmod(header, checksum, 14, 17);
						twamp.pause(100);
						twamp.out.write(header);
						System.out.println("TCP connection established.\n\n");
					}
					break ;
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Server is offline.");
		}
	}
	
	static void end()
	{
		try
		{
			FIN = true ;
			header[13] |= 0x01 ;
			header[13] &= ~0x18 ;
			PSH = false ;
			temp = seq ;
			seq = ack ;
			ack = temp ;
			ack++ ;
			twamp.bytearrmod(header, seq, 4, 7);
			twamp.bytearrmod(header, ack, 8, 11);
			checksum = calculate_checksum();
			twamp.bytearrmod(header, checksum, 14, 17);
			twamp.pause(100);
			twamp.out.write(header);
			while(true)
			{
				twamp.in.read(header);
				if(((header[13]&0x04) >> 2) != 1)
				{
					receive();
					twamp.pause(100);
					display();
					if(ACK)
					{
						temp = seq ;
						seq = ack ;
						ack = temp ;
						ack++ ;
						twamp.bytearrmod(header, seq, 4, 7);
						twamp.bytearrmod(header, ack, 8, 11);
						FIN = false ;
						header[13] &= ~0x01 ;
						checksum = calculate_checksum();
						twamp.bytearrmod(header, checksum, 14, 17);
						twamp.out.write(header);
						System.out.println("\n\n\nTerminating TCP connection.\n");
						twamp.in.close();
						twamp.out.close();
						twamp.pause(1000);
						System.out.println("TCP connection terminated.");
						twamp.socket.close();
						break ;
					}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	static void extract(byte msg[])
	{
		for(int i = 0; i < header.length; i++)
			header[i] = msg[i];
	}
	
	static void send_test_packet() throws IOException
	{
		twamp.bytearrmod(client_test, test_seq, 0, 3);
		test_seq++ ;
		temp3 = (long)(System.currentTimeMillis());
		twamp.long_to_bytearr((long)(temp3/1000)+OFFSET, client_test, 4, 7);
		twamp.long_to_bytearr(((long)(temp3%1e3)*1000000), client_test, 8, 11);
		client_test[12] = (byte)0xb8 ;	// S = 1, Scale = -8
		client_test[13] = 0x4b ;		// Multiplier = 75
		for(int i = 14; i < client_test.length; i++)
			client_test[i] = 0x00 ;
		byte [] client_test_packet = new byte[client_test.length+8];
		for(int i = 0; i < 4; i++)
			client_test_packet[i] = header[i];
		client_test_packet[4] = 0x00 ;
		client_test_packet[5] = (byte)(client_test.length+8);
		client_test_packet[6] = 0x00 ;
		client_test_packet[7] = 0x00 ;
		for(int i = 0; i < client_test.length; i++)
			client_test_packet[i+8] = client_test[i];
		twamp.out.write(client_test_packet);
	}
	
	static void receive_test_packet() throws IOException
	{
		twamp.in.read(server_test_packet);
		now = System.currentTimeMillis()+(OFFSET*1000);
//		now_int = now / 1000 ;
//		now_frac = (now % 1000)*1000000 ;
		System.out.println("Sequence Number : " + twamp.bytearr_to_int(server_test_packet, 8, 11));
//		sst_int = bytearr_to_long(server_test_packet, 12, 15);
//		sst_frac = bytearr_to_long(server_test_packet, 16, 19);
		twamp.date = new Date((twamp.bytearr_to_long(server_test_packet, 12, 15)-OFFSET)*1000+(twamp.bytearr_to_long(server_test_packet, 16, 19)/(long)1e6));
		System.out.println("Timestamp : " + twamp.sdf.format(twamp.date) + "." + String.format("%03d", twamp.bytearr_to_long(server_test_packet, 16, 19)/1000000) + " IST");
//		t2_int = bytearr_to_long(server_test_packet, 24, 27);
//		t2_frac = bytearr_to_long(server_test_packet, 28, 31);
		twamp.date = new Date((now / 1000-OFFSET)*1000+((now % 1000)*1000000/(long)1e6));
		System.out.println("Receive Timestamp : " + twamp.sdf.format(twamp.date) + "." + String.format("%03d", (now % 1000)*1000000/1000000) + " IST");
//		t1_int = bytearr_to_long(server_test_packet, 36, 39);
//		t1_frac = bytearr_to_long(server_test_packet, 40, 43);
		twamp.date = new Date((twamp.bytearr_to_long(server_test_packet, 36, 39)-OFFSET)*1000+(twamp.bytearr_to_long(server_test_packet, 40, 43)/(long)1e6));
		System.out.println("Sender Timestamp : " + twamp.sdf.format(twamp.date) + "." + String.format("%03d", twamp.bytearr_to_long(server_test_packet, 40, 43)/1000000) + " IST");
		if((now % 1000)*1000000-twamp.bytearr_to_long(server_test_packet, 40, 43) < 1000000 && twamp.bytearr_to_long(server_test_packet, 36, 39) == now / 1000)
		{
			System.out.println("time < 1 ms\n\n");
			min_rtt = 0 ;
		}
		else if(((now / 1000-twamp.bytearr_to_long(server_test_packet, 36, 39))*1000+((now % 1000)*1000000-twamp.bytearr_to_long(server_test_packet, 40, 43))/1000000) < timeout_int*1000+timeout_frac/1000000)
		{
			System.out.println("time = "  +  ((now / 1000-twamp.bytearr_to_long(server_test_packet, 36, 39))*1000 +((now % 1000)*1000000-twamp.bytearr_to_long(server_test_packet, 40, 43))/1000000 + " ms\n\n"));
			total_rtt += ((now / 1000-twamp.bytearr_to_long(server_test_packet, 36, 39))*1000 +((now % 1000)*1000000-twamp.bytearr_to_long(server_test_packet, 40, 43))/1000000);
			if(max_rtt < ((now / 1000-twamp.bytearr_to_long(server_test_packet, 36, 39))*1000 +((now % 1000)*1000000-twamp.bytearr_to_long(server_test_packet, 40, 43))/1000000))
				max_rtt = ((now / 1000-twamp.bytearr_to_long(server_test_packet, 36, 39))*1000 +((now % 1000)*1000000-twamp.bytearr_to_long(server_test_packet, 40, 43))/1000000);
			if(min_rtt > ((now / 1000-twamp.bytearr_to_long(server_test_packet, 36, 39))*1000 +((now % 1000)*1000000-twamp.bytearr_to_long(server_test_packet, 40, 43))/1000000))
				min_rtt = ((now / 1000-twamp.bytearr_to_long(server_test_packet, 36, 39))*1000 +((now % 1000)*1000000-twamp.bytearr_to_long(server_test_packet, 40, 43))/1000000);
		}
		else
		{
			System.out.println("Request timed out\n\n");
			packets_lost++ ;
		}
		
		if(glv == 0)
		{
			start_int = now / 1000 ;
			start_frac = (now % 1000)*1000000 ;
		}
		if(glv == packets_sent-1)
		{
			end_int = now / 1000 ;
			end_frac = (now % 1000)*1000000 ;
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		String [] tempip1 = Inet4Address.getLocalHost().getHostAddress().split(("\\."));
		byte [] client_ip = new byte[tempip1.length];
		for(int i = 0; i < client_ip.length; i++)
			client_ip[i] = (byte)(Integer.parseInt(tempip1[i]));
		System.out.print("Enter IP address of server : ");
		String t3mp = twamp.sc.nextLine();
		String [] tempip2 = t3mp.split("\\.");
		byte [] server_ip = new byte[tempip2.length];
		for(int i = 0; i < server_ip.length; i++)
			server_ip[i] = (byte)(Integer.parseInt(tempip2[i]));
		new client(t3mp, 862);
		twamp.sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+5:30")); 
		byte [] server_greeting_message = new byte[84];
		twamp.in.read(server_greeting_message);
		extract(server_greeting_message);
		receive();
		twamp.pause(100);
		display();
		System.out.println("Received server greeting message.");
		System.out.println("Modes : ");
		System.out.print("Unauthenticated : ");
		if((server_greeting_message[35]&0x01) == 0)
			System.out.println("No");
		else
			System.out.println("Yes");
		System.out.print("Authenticated : ");
		if((server_greeting_message[35]&0x02) == 0)
			System.out.println("No");
		else
			System.out.println("Yes");
		System.out.print("Encrypted : ");
		if((server_greeting_message[35]&0x04) == 0)
			System.out.println("No");
		else
			System.out.println("Yes");
		System.out.println("Challenge : " + twamp.bytesToHex(server_greeting_message, 36, 51));
		System.out.println("Salt : " + twamp.bytesToHex(server_greeting_message, 52, 67));
		byte [] set_up_response = new byte[184];
		temp = seq ;
		seq = ack ;
		ack = temp ;
		ack += (server_greeting_message.length-header.length);
		twamp.bytearrmod(header, seq, 4, 7);
		twamp.bytearrmod(header, ack, 8, 11);
		for(int i = 0; i < header.length; i++)
			set_up_response[i] = header[i];
		for(int i = header.length; i < set_up_response.length; i++)
			set_up_response[i] = 0x00 ;
		set_up_response[23] = 0x01 ;
		checksum = calculate_checksum();
		twamp.bytearrmod(set_up_response, checksum, 14, 17);
		twamp.out.write(set_up_response);
		System.out.println("\n\n");
		byte [] server_start_message = new byte[68];
		twamp.in.read(server_start_message);
		extract(server_start_message);
		receive();
		twamp.pause(100);
		display();
		System.out.println("Received server start message.");
//		sst_int = bytearr_to_long(server_start_message, 52, 55);
//		sst_frac = bytearr_to_long(server_start_message, 56, 59);
		twamp.date = new Date((twamp.bytearr_to_long(server_start_message, 52, 55)-OFFSET)*1000+(twamp.bytearr_to_long(server_start_message, 56, 59)/(long)1e6));
		twamp.sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+5:30")); 
		System.out.println("Server Start Time : " + twamp.sdf.format(twamp.date) + "." + twamp.bytearr_to_long(server_start_message, 56, 59)/1000000 + " IST\n\n");
		byte [] request_session = {0x05,																								// Request-TW-Session
								   0x04,																								// IP version
								   0x00, 0x00,																							// Conf-Sender and Conf-Receiver
								   0x00, 0x00, 0x00, 0x00,																				// Number of Scheduled Slots
								   0x00, 0x00, 0x00, 0x00,																				// Number of Packets
								   0x00, 0x00,																							// Sender Port
								   0x00, 0x00,																							// Receiver Port
								   0x00, 0x00, 0x00, 0x00,																				// Sender Address
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,								// MBZ because IPv4
								   0x00, 0x00, 0x00, 0x00,																				// Receiver Address
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,								// MBZ because IPv4
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// SID
								   0x00, 0x00, 0x00, 0x00,																				// Padding Length
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,														// start-time
								   0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00,														// timeout = 5.0 seconds
								   0x00, 0x00, 0x00, 0x00,																				// Type-P Descriptor
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,														// MBZ
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// HMAC (MBZ because of unauthenciated mode)
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};		// HMAC (MBZ because of unauthenciated mode)
		for(int i = 0; i < client_ip.length; i++)
			request_session[i+16] = client_ip[i];
		for(int i = 0; i < server_ip.length; i++)
			request_session[i+32] = server_ip[i];
		timeout_int = twamp.bytearr_to_long(request_session, 76, 79);
		timeout_frac = twamp.bytearr_to_long(request_session, 80, 83);
		temp3 = (long)(System.currentTimeMillis());
		// manually writing timestamp in bytes cause Java sucks, doesn't handle unsigned int
		twamp.long_to_bytearr((long)(temp3/1000)+OFFSET, request_session, 68, 71);
		twamp.long_to_bytearr(((long)(temp3%1e3)*1000000), request_session, 72, 75);
		twamp.int_to_bytearr(src_port, request_session, 12, 13);
		twamp.int_to_bytearr(dest_port, request_session, 14, 15);
		byte [] request_session_message = new byte[header.length+request_session.length];
		temp = seq ;
		seq = ack ;
		ack = temp ;
		ack += (server_start_message.length-header.length);
		twamp.bytearrmod(header, seq, 4, 7);
		twamp.bytearrmod(header, ack, 8, 11);
		for(int i = 0; i < header.length; i++)
			request_session_message[i] = header[i];
		for(int i = 0; i < request_session.length; i++)
			request_session_message[i+header.length] = request_session[i];
		checksum = calculate_checksum();
		twamp.bytearrmod(request_session_message, checksum, 14, 17);
		twamp.out.write(request_session_message);
		byte [] accept_session_message = new byte[68];
		twamp.in.read(accept_session_message);
		extract(accept_session_message);
		receive();
		twamp.pause(100);
		display();
		System.out.println("SID : " + twamp.bytesToHex(accept_session_message, 24, 40));
		System.out.println("Received accept session message.\n\n");
		byte [] start_sessions = new byte[32];
		for(int i = 0; i < start_sessions.length; i++)
			start_sessions[i] = 0x00 ;
		start_sessions[0] = 0x02 ;	// Control Command (start sessions)
		byte [] start_sessions_message = new byte[header.length+start_sessions.length];
		temp = seq ;
		seq = ack ;
		ack = temp ;
		ack += (accept_session_message.length-header.length);
		twamp.bytearrmod(header, seq, 4, 7);
		twamp.bytearrmod(header, ack, 8, 11);
		for(int i = 0; i < header.length; i++)
			start_sessions_message[i] = header[i];
		for(int i = 0; i < start_sessions.length; i++)
			start_sessions_message[i+header.length] = start_sessions[i];
		checksum = calculate_checksum();
		twamp.bytearrmod(start_sessions_message, checksum, 14, 17);
		twamp.out.write(start_sessions_message);
		byte [] start_ack = new byte[52];
		twamp.in.read(start_ack);
		extract(start_ack);
		receive();
		twamp.pause(100);
		display();
		System.out.println("Start ACK received.\n\n");
		System.out.print("Enter number of test packets to send : ");
		packets_sent = twamp.sc.nextInt();
		twamp.out.writeInt(packets_sent);
		if(packets_sent >= 1)
		{
			for(glv = 0; glv < packets_sent; glv++)
			{
				send_test_packet();
				receive_test_packet();
			}
			System.out.println("    Packets: Sent = " + packets_sent + ", Received = " + (packets_sent-packets_lost) + ", Lost = " + packets_lost + " (" + (packets_lost/packets_sent)*100.00 + "% loss)");
			System.out.println("Approximate round trip times in milli-seconds:");
			System.out.println("    Minimum = " + min_rtt + " ms, Maximum = " + max_rtt + " ms, Average = " + (total_rtt/packets_sent) + " ms");
			if(packets_sent > 1)
				System.out.println("Average Jitter = " + String.format("%.3f", (float)(((end_int*1000+end_frac/1000000)-(start_int*1000+start_frac/1000000)))/(packets_sent-1)) + " ms");
			else
				System.out.println("Cannot calculate jitter for just one test packet sent and received.");
		}
		byte [] stop_sessions = {0x03,																								// 3 for stop session
								 0x00,																								// Accept (0 means no errors)
								 0x00, 0x00,																						// MBZ
								 0x00, 0x00, 0x00, 0x01,																			// Number of sessions
								 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,													// MBZ
								 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};	// HMAC
		byte [] stop_sessions_command = new byte[stop_sessions.length+header.length];
		temp = seq ;
		seq = ack ;
		ack = temp ;
		ack += (52-header.length);
		twamp.bytearrmod(header, seq, 4, 7);
		twamp.bytearrmod(header, ack, 8, 11);
		for(int i = 0; i < header.length; i++)
			stop_sessions_command[i] = header[i];
		for(int i = 0; i < stop_sessions.length; i++)
			stop_sessions_command[i+header.length] = stop_sessions[i];
		checksum = calculate_checksum();
		twamp.bytearrmod(stop_sessions_command, checksum, 14, 17);
		twamp.out.write(stop_sessions_command);
		System.out.println("\n\n\nStopping current TWAMP session.\n\n");
		end();
	}
}