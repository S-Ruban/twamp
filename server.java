import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.Date;

import twamp.twamp;

public class server
{	
	static Boolean URG, ACK, PSH, RST, SYN, FIN ;
	static int src_port, dest_port, seq, ack, HLEN, win_size, checksum, urg_ptr, temp, test_seq, packets_received, base_seq, base_ack ;
	
	static byte [] header = new byte[20];
	
	static byte [] client_test_packet = new byte[49];
	static byte [] server_test_packet = new byte[client_test_packet.length];
	
	server(int port) throws IOException
	{
		start(port);
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
	
	static void start(int p)
	{
		try
		{
			twamp.server = new ServerSocket(p);
			System.out.println("Waiting for client.");
			twamp.socket = twamp.server.accept();
			System.out.println("Client is online.\n\n");
			twamp.in = new DataInputStream(twamp.socket.getInputStream());
			twamp.out = new DataOutputStream(twamp.socket.getOutputStream());
			while(true)
			{
				twamp.in.read(header);
				if((header[13]&0x01) != 1)
				{
					receive();
					twamp.pause(100);
					display();
					System.out.println("\n\n");
					if(SYN && !ACK)
					{
						
						ACK = true ;
						header[13] |= 0x10 ;
						base_ack = seq ;
						ack = seq ;
						seq = twamp.random.nextInt(Short.MAX_VALUE);
						base_seq = seq ;
						ack++ ;
						twamp.bytearrmod(header, seq, 4, 7);
						twamp.bytearrmod(header, ack, 8, 11);
						checksum = calculate_checksum();
						twamp.bytearrmod(header, checksum, 14, 17);
					}
					if(!SYN && ACK)
					{
						if(checksum == calculate_checksum())
						{
							twamp.pause(100);
							System.out.println("TCP connection established.\n\n");
							PSH = true ;
							header[13] |= 0x18 ;
						}
						break ;
					}
					twamp.pause(100);
					twamp.out.write(header);
				}
			}
		}
		catch(IOException ioe)
		{
			System.out.println("\nClient has disconnected.\n\n");
		}
	}
	
	static void end()
	{
		try
		{
			while(true)
			{
				twamp.in.read(header);
				if(((header[13]&0x04) >> 2) != 1)
				{
					temp = seq ;
					seq = ack ;
					ack = temp ;
					ack++ ;
					twamp.bytearrmod(header, seq, 4, 7);
					twamp.bytearrmod(header, ack, 8, 11);
					receive();
					twamp.pause(100);
					display();
					System.out.println("\n\n");
					header[13] |= 0x11 ;
					checksum = calculate_checksum();
					twamp.bytearrmod(header, checksum, 14, 17);
					twamp.pause(100);
					twamp.out.write(header);
					if(ACK)
					{
						twamp.in.close();
						twamp.out.close();
						System.out.println("TCP connection terminated.");
						twamp.socket.close();
						twamp.server.close();
						break ;
					}
				}
			}
		}
		catch(IOException ioe)
		{
			System.out.println("\nClient has disconnected.\n\n");
		}
	}
	
	static void reflect() throws IOException
	{
		int c = 0 ;
		while(true)
		{
			if(c == packets_received)
				break ;
			twamp.in.read(client_test_packet);
			twamp.temp3 = (long)(System.currentTimeMillis());
//			temp2 = ((long)(temp3%1e3)*1000000);
//			temp1 = (long)(temp3/1000)+OFFSET ;
			twamp.long_to_bytearr((long)(twamp.temp3/1000)+twamp.OFFSET, server_test_packet, 12, 15);
			twamp.long_to_bytearr(((long)(twamp.temp3%1e3)*1000000), server_test_packet, 16, 19);
			for(int i = 12; i < 20; i++)
				server_test_packet[i+12] = server_test_packet[i];					// Receive Timestamp
			System.out.println("Sequence Number : " + twamp.bytearr_to_int(client_test_packet, 8, 11));
//			sst_int = bytearr_to_long(client_test_packet, 12, 15);
//			sst_frac = bytearr_to_long(client_test_packet, 16, 19);
			twamp.date = new Date((twamp.bytearr_to_long(client_test_packet, 12, 15)-twamp.OFFSET)*1000+(twamp.bytearr_to_long(client_test_packet, 16, 19)/(long)1e6));
			System.out.println("Timestamp : " + twamp.sdf.format(twamp.date) + "." + String.format("%03d", twamp.bytearr_to_long(client_test_packet, 16, 19)/1000000) + " IST\n");
			for(int i = 0; i < 4; i++)
				server_test_packet[i] = client_test_packet[(i+2)%4];		// exchange source and destination ports in UDP header
			for(int i = 4; i < 12; i++)
				server_test_packet[i] = client_test_packet[i];				// copy same UDP packet length, checksum (lite) and sequence number of TWAMP test packet
			for(int i = 20; i < 24; i++)
				server_test_packet[i] = client_test_packet[i];				// copy Error Estimate and MBZ
			for(int i = 8; i < 24; i++)
				server_test_packet[i+24] = client_test_packet[i];			// copy timestamp in client test packet into Sender Timestamp, along with Error Estimate
			server_test_packet[server_test_packet.length-1] = (byte)0xff ;	// set TTL to 255
			twamp.out.write(server_test_packet);
			c++ ;
		}
	}
	
	static void extract(byte msg[])
	{
		for(int i = 0; i < header.length; i++)
			header[i] = msg[i];
	}
	
	public static void main(String[] args) throws IOException
	{
		twamp.temp3 = (long)(System.currentTimeMillis());
		SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss z");
		sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+5:30"));
		Date date = new Date(0);
		server s = new server(862);
		byte [] server_greeting = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,								// unused, so leave it
								   0x00, 0x00, 0x00, 0x01,																				// 1 for unauthenticated, 2 for authenticated, and 4 for encrypted
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// challenge, random number
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// salt, psuedo-random
								   0x00, 0x00, 0x04, 0x00,																				// count, must atleast be 1024
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};								// MBZ
		byte [] challenge = new byte[16];
		byte [] salt = new byte[16];
		twamp.random.nextBytes(challenge);
		twamp.random.nextBytes(salt);
		for(int i = 0; i < challenge.length; i++)
		{
			server_greeting[i+16] = challenge[i];
			server_greeting[i+32] = salt[i];
		}
		byte [] server_greeting_message = new byte[header.length+server_greeting.length];
		checksum = calculate_checksum();
		for(int i = 0; i < header.length; i++)
			server_greeting_message[i] = header[i];
		for(int i = 0; i < server_greeting.length; i++)
			server_greeting_message[header.length+i] = server_greeting[i];
		checksum = calculate_checksum();
		twamp.bytearrmod(server_greeting_message, checksum, 14, 17);
		twamp.out.write(server_greeting_message);
		byte [] set_up_response = new byte[184];
		twamp.in.read(set_up_response);
		extract(set_up_response);
		receive();
		twamp.pause(100);
		display();
		System.out.println("Received set-up response.\n\n");
		byte [] server_start = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// MBZ (last octet being 0x00 implies no problem)
								0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// Server-IV (unused in unauthenticated mode)
								0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,														// timestamp of server start
								0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};													// MBZ
//		temp2 = ((long)(temp3%1e3)*1000000);
//		temp1 = (long)(temp3/1000)+OFFSET ;
		twamp.long_to_bytearr((long)(twamp.temp3/1000)+twamp.OFFSET, server_start, 32, 35);
		twamp.long_to_bytearr(((long)(twamp.temp3%1e3)*1000000), server_start, 36, 39);
		byte [] server_start_message = new byte[header.length+server_start.length];
		temp = seq ;
		seq = ack ;
		ack = temp ;
		ack += (set_up_response.length-header.length);
		twamp.bytearrmod(header, seq, 4, 7);
		twamp.bytearrmod(header, ack, 8, 11);
		for(int i = 0; i < header.length; i++)
			server_start_message[i] = header[i];
		for(int i = 0; i < server_start.length; i++)
			server_start_message[i+header.length] = server_start[i];
		checksum = calculate_checksum();
		twamp.bytearrmod(server_start_message, checksum, 14, 17);
		twamp.out.write(server_start_message);
		byte [] request_session_message = new byte[148];
		twamp.in.read(request_session_message);
		extract(request_session_message);
		receive();
		twamp.pause(100);
		display();
		System.out.println("IP Version : IPv" + request_session_message[21]);
		System.out.println("Sender Port : " + twamp.bytearr_to_int(request_session_message, 32, 33));
		System.out.println("Receiver Port : " + twamp.bytearr_to_int(request_session_message, 34, 35));
		System.out.print("Sender Address : ");
		for(int i = 36; i < 39; i++)
			System.out.print(twamp.bytearr_to_int(request_session_message, i, i) + ".");	// I need to use a separate function just because Java doesn't support unsigned values smh
		System.out.println(twamp.bytearr_to_int(request_session_message, 39, 39));
		System.out.print("Receiver Address : ");
		for(int i = 52; i < 55; i++)
			System.out.print(twamp.bytearr_to_int(request_session_message, i, i) + ".");
		System.out.println(twamp.bytearr_to_int(request_session_message, 55, 55));
//		sst_int = bytearr_to_long(request_session_message, 88, 91);
//		sst_frac = bytearr_to_long(request_session_message, 92, 95);
		date = new Date((twamp.bytearr_to_long(request_session_message, 88, 91)-twamp.OFFSET)*1000+(twamp.bytearr_to_long(request_session_message, 92, 95)/(long)1e6));
		System.out.println("Start Time : " + sdf.format(date).substring(0, sdf.format(date).length()-10) + "." + String.format("%03d", twamp.bytearr_to_long(request_session_message, 92, 95)/1000000) + " IST\n");
		System.out.println("Received request session message.\n\n");
		byte [] accept_session = new byte[48];
		accept_session[0] = 0x00 ;	// Accept
		accept_session[1] = 0x00 ;	// MBZ
		for(int i = 2; i < 4; i++)
			accept_session[i] = request_session_message[i+30];	// Port number
		for(int i = 4; i < 8; i++)
			accept_session[i] = request_session_message[i+48];	// IP of generating machine
		twamp.temp3 = (long)(System.currentTimeMillis());
		twamp.long_to_bytearr((long)(twamp.temp3/1000)+twamp.OFFSET, accept_session, 8, 11);
		twamp.long_to_bytearr(((long)(twamp.temp3%1e3)*1000000), accept_session, 12, 15);
		byte [] r = new byte[4];
		twamp.random.nextBytes(r);
		for(int i = 0; i < r.length; i++)
			accept_session[i+16] = r[i];
		for(int i = 20; i < 32; i++)
			accept_session[i] = 0x00 ;	// MBZ
		for(int i = 32; i < 48; i++)
			accept_session[i] = 0x00 ;  // HMAC (MBZ because of unauthenciated mode)
		byte [] accept_session_message = new byte[header.length+accept_session.length];
		temp = seq ;
		seq = ack ;
		ack = temp ;
		ack += (request_session_message.length-header.length);
		twamp.bytearrmod(header, seq, 4, 7);
		twamp.bytearrmod(header, ack, 8, 11);
		for(int i = 0; i < header.length; i++)
			accept_session_message[i] = header[i];
		for(int i = 0; i < accept_session.length; i++)
			accept_session_message[i+header.length] = accept_session[i];
		checksum = calculate_checksum();
		twamp.bytearrmod(accept_session_message, checksum, 14, 17);
		twamp.out.write(accept_session_message);
		byte [] start_sessions_message = new byte[52];
		twamp.in.read(start_sessions_message);
		extract(start_sessions_message);
		receive();
		twamp.pause(100);
		display();
		System.out.println("Start sessions message received.\n\n");
		byte [] start_ack = new byte[start_sessions_message.length];
		temp = seq ;
		seq = ack ;
		ack = temp ;
		ack += (start_sessions_message.length-header.length);
		twamp.bytearrmod(header, seq, 4, 7);
		twamp.bytearrmod(header, ack, 8, 11);
		for(int i = 0; i < header.length; i++)
			start_ack[i] = header[i];
		for(int i = 0; i < start_sessions_message.length-header.length; i++)
			start_ack[i+header.length] = start_sessions_message[i+header.length];
		start_ack[20] = 0x00 ;
		checksum = calculate_checksum();
		twamp.bytearrmod(start_ack, checksum, 14, 17);
		twamp.out.write(start_ack);
		packets_received = twamp.in.readInt();
		if(packets_received > 0)
			reflect();
		byte [] stop_sessions_command = new byte[52];
		twamp.in.read(stop_sessions_command);
		extract(stop_sessions_command);
		receive();
		twamp.pause(100);
		display();
		System.out.println("Stopping current TWAMP session.\n\n");
		end();
	}
}