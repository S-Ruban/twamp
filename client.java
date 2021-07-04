import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class client
{
	static Socket socket = null;
	static DataInputStream in = null;
	static DataOutputStream out = null;
	static Random random = new Random();
	private static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
	
	static Boolean URG = false, ACK = false, PSH = false, RST = false, SYN = true, FIN = false ;
	static int src_port = 862, dest_port = 862, seq = 0, ack = 0, HLEN = 5, win_size = 0, checksum = 0, urg_ptr = 0, TIME_WAIT = 5000, temp, test_seq = 0, packets_sent = 10, packets_lost = 0, glv ;
	static long OFFSET = 2208988800L, sst_int = 0, sst_frac = 0, temp1, temp2, t1_int, t1_frac, t2_int, t2_frac, temp3, start_int, start_frac, end_int, end_frac ;
	
	static byte [] client_test = new byte[41];
	static byte [] server_test_packet = new byte[client_test.length+8];
	
	static Date date = new Date(0);
	static SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	
	
	static byte [] header = {0x03, 0x5e,				// source port
					  		 0x03, 0x5e,				// dest port
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
	
	static void bytearrmod(byte [] arr, int val, int s, int e)
	{
		ByteBuffer bb = ByteBuffer.allocate(e-s+1);
		bb.putInt(val);
		byte[] temp = bb.array();
		for(int i = 0; i <= e-s; i++)
			arr[i+s] = temp[i];
	}
	
	static int bytearr_to_int(byte arr[], int s, int e)
	{
		int res = 0 ;
		for(int i = s; i <= e; i++)
		{
			if((int)header[i] >= 0)
				res = res*256 + (int)arr[i];
			else
				res = res*256 + (int)arr[i] + 256 ;
		}
		return res ;
	}
	
	static void header()
	{
		bytearrmod(header, seq, 4, 7);
		bytearrmod(header, ack, 8, 11);
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
				checksum += (65535-bytearr_to_int(header, x, x+1));
		}
		checksum = (~checksum) & 0xffff ;
		bytearrmod(header, checksum, 14, 17);
		header[14] = 0 ;
		header[15] = 0 ;
	}
	
	static void receive()
	{
		src_port = bytearr_to_int(header, 0,1);
		dest_port = bytearr_to_int(header, 2,3);
		seq = bytearr_to_int(header, 4,7);
		ack = bytearr_to_int(header, 8,11);
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
		win_size = bytearr_to_int(header, 14,15);
		checksum = bytearr_to_int(header, 16,17);
		urg_ptr = bytearr_to_int(header, 18,19);
	}
	
	static int calculate_checksum()
	{
		int cs = 0 ;
		for(int x = 0; x < 20; x += 2)
		{
			if(x == 16)
				continue ;
			else
				cs += (65535-bytearr_to_int(header, x, x+1));
		}
		cs = (~cs) & 0xffff ;
		return cs ;
	}
	
	static void display()
	{
		System.out.println("Source port : " + src_port);
		System.out.println("Destination port : " + dest_port);
		System.out.println("Sequence number : " + seq);
		System.out.println("Acknowledgement number : " + ack);
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
	
	static void pause(int ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch(InterruptedException ie)
		{
			System.out.println(ie);
		}
	}
	
	static void start(String i, int p)
	{
		try
		{
			socket = new Socket(i, p);
			System.out.println("Server is online.\n\n");
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			header();
			out.write(header);
			while(true)
			{
				in.read(header);
				if((header[13]&0x01) != 1)
				{
					receive();
					pause(100);
					display();
					System.out.println("\n\n");
					if(SYN && ACK)
					{
						temp = seq ;
						seq = ack ;
						ack = temp ;
						ack++ ;
						SYN = false ;
						header[13] &= ~0x02 ;
						bytearrmod(header, seq, 4, 7);
						bytearrmod(header, ack, 8, 11);
						checksum = calculate_checksum();
						bytearrmod(header, checksum, 14, 17);
						pause(100);
						out.write(header);
						System.out.println("TCP connection established.\n\n");
					}
					break ;
				}
			}
		}
		catch(IOException ioe)
		{
			System.out.println("\nServer has disconnected.\n\n");
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
			bytearrmod(header, seq, 4, 7);
			bytearrmod(header, ack, 8, 11);
			checksum = calculate_checksum();
			bytearrmod(header, checksum, 14, 17);
			pause(100);
			out.write(header);
			while(true)
			{
				in.read(header);
				if(((header[13]&0x04) >> 2) != 1)
				{
					receive();
					pause(100);
					display();
					if(ACK)
					{
						temp = seq ;
						seq = ack ;
						ack = temp ;
						ack++ ;
						bytearrmod(header, seq, 4, 7);
						bytearrmod(header, ack, 8, 11);
						FIN = false ;
						header[13] &= ~0x01 ;
						checksum = calculate_checksum();
						bytearrmod(header, checksum, 14, 17);
						out.write(header);
						System.out.println("\n\n\nTerminating TCP connection.\n");
						in.close();
						out.close();
						pause(TIME_WAIT);
						System.out.println("TCP connection terminated.");
						socket.close();
						break ;
					}
				}
			}
		}
		catch(IOException ioe)
		{
			System.out.println("\nServer has disconnected.\n\n");
		}
	}
	
	static void extract(byte msg[])
	{
		for(int i = 0; i < header.length; i++)
			header[i] = msg[i];
	}
	
	static void send_test_packet() throws IOException
	{
		bytearrmod(client_test, test_seq, 0, 3);
		test_seq++ ;
		temp3 = (long)(System.currentTimeMillis());
		temp2 = ((long)(temp3%1e3)*1000000);
		temp1 = (long)(temp3/1000)+OFFSET ;
		client_test[4] = (byte)((temp1 & 0x00000000FF000000L) >> 24);
		client_test[5] = (byte)((temp1 & 0x0000000000FF0000L) >> 16);
		client_test[6] = (byte)((temp1 & 0x000000000000FF00L) >> 8);
		client_test[7] = (byte)((temp1 & 0x00000000000000FFL));
		client_test[8] = (byte)((temp2 & 0x00000000FF000000L) >> 24);
		client_test[9] = (byte)((temp2 & 0x0000000000FF0000L) >> 16);
		client_test[10] = (byte)((temp2 & 0x000000000000FF00L) >> 8);
		client_test[11] = (byte)((temp2 & 0x00000000000000FFL));
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
		out.write(client_test_packet);
	}
	
	static void receive_test_packet() throws IOException
	{
		while(true)
		{
			in.read(server_test_packet);
			System.out.println("Sequence Number : " + bytearr_to_int(server_test_packet, 8, 11));
			sst_int = 0 ;
			sst_frac = 0 ;
			for(int i = 12; i < 16; i++)
			{
				if((int)server_test_packet[i] >= 0)
					sst_int = sst_int*256 + server_test_packet[i];
				else
					sst_int = sst_int*256 + server_test_packet[i]+256 ;
			}
			for(int i = 16; i < 20; i++)
			{
				if((int)server_test_packet[i] >= 0)
					sst_frac = sst_frac*256 + server_test_packet[i];
				else
					sst_frac = sst_frac*256 + server_test_packet[i]+256 ;
			}
			date = new Date((sst_int-OFFSET)*1000+(sst_frac/(long)1e6));
			System.out.println("Timestamp : " + sdf.format(date) + "." + sst_frac/1000000 + " IST");
			t2_int = 0 ;
			t2_frac = 0 ;
			for(int i = 24; i < 28; i++)
			{
				if((int)server_test_packet[i] >= 0)
					t2_int = t2_int*256 + server_test_packet[i];
				else
					t2_int = t2_int*256 + server_test_packet[i]+256 ;
			}
			for(int i = 28; i < 32; i++)
			{
				if((int)server_test_packet[i] >= 0)
					t2_frac = t2_frac*256 + server_test_packet[i];
				else
					t2_frac = t2_frac*256 + server_test_packet[i]+256 ;
			}
			date = new Date((t2_int-OFFSET)*1000+(t2_frac/(long)1e6));
			System.out.println("Receive Timestamp : " + sdf.format(date) + "." + t2_frac/1000000 + " IST");
			t1_int = 0 ;
			t1_frac = 0 ;
			for(int i = 36; i < 40; i++)
			{
				if((int)server_test_packet[i] >= 0)
					t1_int = t1_int*256 + server_test_packet[i];
				else
					t1_int = t1_int*256 + server_test_packet[i]+256 ;
			}
			for(int i = 40; i < 44; i++)
			{
				if((int)server_test_packet[i] >= 0)
					t1_frac = t1_frac*256 + server_test_packet[i];
				else
					t1_frac = t1_frac*256 + server_test_packet[i]+256 ;
			}
			date = new Date((t1_int-OFFSET)*1000+(t1_frac/(long)1e6));
			System.out.println("Sender Timestamp : " + sdf.format(date) + "." + t1_frac/1000000 + " IST");
			if(t2_frac-t1_frac < 1000000 && t1_int == t2_int)
				System.out.println("time < 1 ms\n\n");
			else if(t2_int - t1_int < 4)	// 4s is defined as the threshold for ping to not timeout (atleast in Windows)
				System.out.println("time = "  + (t2_frac-t1_frac)/1000000 + " ms\n\n");
			else
			{
				System.out.println("Request timed out\n\n");
				packets_lost++ ;
			}
			if(glv == 0)
			{
				start_int = t2_int ;
				start_frac = t2_frac ;
			}
			if(glv == packets_sent-1)
			{
				end_int = t2_int ;
				end_frac = t2_frac ;
			}
			break ;
		}
	}
	
	public static String bytesToHex(byte[] arr, int s, int e)
	{
	    char[] hexChars = new char[(e-s+1)*2];
	    for (int j = s; j <= e; j++)
	    {
	        int v = arr[j] & 0xFF ;
	        hexChars[(j-s)*2] = HEX_ARRAY[v >>> 4];
	        hexChars[(j-s)*2+1] = HEX_ARRAY[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static void main(String[] args) throws IOException
	{
		client c = new client("192.168.1.3", 862);
		sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+5:30")); 
		byte [] server_greeting_message = new byte[84];
		while(true)
		{
			in.read(server_greeting_message);
			extract(server_greeting_message);
			receive();
			pause(100);
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
			System.out.println("Challenge : " + bytesToHex(server_greeting_message, 36, 51));
			System.out.println("Salt : " + bytesToHex(server_greeting_message, 52, 67));
			byte [] set_up_response = new byte[184];
			temp = seq ;
			seq = ack ;
			ack = temp ;
			ack += (server_greeting_message.length-header.length);
			bytearrmod(header, seq, 4, 7);
			bytearrmod(header, ack, 8, 11);
			for(int i = 0; i < header.length; i++)
				set_up_response[i] = header[i];
			for(int i = header.length; i < set_up_response.length; i++)
				set_up_response[i] = 0x00 ;
			set_up_response[23] = 0x01 ;
			checksum = calculate_checksum();
			bytearrmod(set_up_response, checksum, 14, 17);
			out.write(set_up_response);
			System.out.println("\n\n");
			break ;
		}
		byte [] server_start_message = new byte[68];
		while(true)
		{
			in.read(server_start_message);
			extract(server_start_message);
			receive();
			pause(100);
			display();
			System.out.println("Received server start message.");
			for(int i = 52; i < 56; i++)
			{
				if((int)server_start_message[i] >= 0)
					sst_int = sst_int*256 + server_start_message[i];
				else
					sst_int = sst_int*256 + server_start_message[i]+256 ;
			}
			for(int i = 56; i < 60; i++)
			{
				if((int)server_start_message[i] >= 0)
					sst_frac = sst_frac*256 + server_start_message[i];
				else
					sst_frac = sst_frac*256 + server_start_message[i]+256 ;
			}
			date = new Date((sst_int-OFFSET)*1000+(sst_frac/(long)1e6));
			sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+5:30")); 
			System.out.println("Server Start Time : " + sdf.format(date) + "." + sst_frac/1000000 + " IST\n\n");
			byte [] request_session = {0x05,																								// Request-TW-Session
									   0x04,																								// IP version
									   0x00, 0x00,																							// Conf-Sender and Conf-Receiver
									   0x00, 0x00, 0x00, 0x00,																				// Number of Scheduled Slots
									   0x00, 0x00, 0x00, 0x00,																				// Number of Packets
									   0x03, 0x5e,																							// Sender Port
									   0x03, 0x5e,																							// Receiver Port
									   (byte) 0xc0, (byte) 0xa8, 0x01, 0x05,																// Sender Address
									   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,								// MBZ because IPv4
									   (byte) 0xc0, (byte) 0xa8, 0x01, 0x05,																// Receiver Address
									   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,								// MBZ because IPv4
									   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// SID
									   0x00, 0x00, 0x00, 0x00,																				// Padding Length
									   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,														// start-time
									   0x00, 0x00, 0x00, 0x05, 0x00, 0x00, 0x00, 0x00,														// timeout = 5.0 seconds
									   0x00, 0x00, 0x00, 0x00,																				// Type-P Descriptor
									   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,														// MBZ
									   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// HMAC (MBZ because of unauthenciated mode)
									   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};		// HMAC (MBZ because of unauthenciated mode)
			temp3 = (long)(System.currentTimeMillis());
			temp2 = ((long)(temp3%1e3)*1000000);
			temp1 = (long)(temp3/1000)+OFFSET ;
			// manually writing timestamp in bytes cause Java sucks, doesn't handle unsigned int
			request_session[68] = (byte)((temp1 & 0x00000000FF000000L) >> 24);
			request_session[69] = (byte)((temp1 & 0x0000000000FF0000L) >> 16);
			request_session[70] = (byte)((temp1 & 0x000000000000FF00L) >> 8);
			request_session[71] = (byte)((temp1 & 0x00000000000000FFL));
			request_session[72] = (byte)((temp2 & 0x00000000FF000000L) >> 24);
			request_session[73] = (byte)((temp2 & 0x0000000000FF0000L) >> 16);
			request_session[74] = (byte)((temp2 & 0x000000000000FF00L) >> 8);
			request_session[75] = (byte)((temp2 & 0x00000000000000FFL));
			byte [] request_session_message = new byte[header.length+request_session.length];
			temp = seq ;
			seq = ack ;
			ack = temp ;
			ack += (server_start_message.length-header.length);
			bytearrmod(header, seq, 4, 7);
			bytearrmod(header, ack, 8, 11);
			for(int i = 0; i < header.length; i++)
			request_session_message[i] = header[i];
			for(int i = 0; i < request_session.length; i++)
			request_session_message[i+header.length] = request_session[i];
			checksum = calculate_checksum();
			bytearrmod(request_session_message, checksum, 14, 17);
			out.write(request_session_message);
			break ;
		}
		byte [] accept_session_message = new byte[68];
		while(true)
		{
			in.read(accept_session_message);
			extract(accept_session_message);
			receive();
			pause(100);
			display();
			System.out.println("SID : " + bytesToHex(accept_session_message, 24, 40));
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
			bytearrmod(header, seq, 4, 7);
			bytearrmod(header, ack, 8, 11);
			for(int i = 0; i < header.length; i++)
				start_sessions_message[i] = header[i];
			for(int i = 0; i < start_sessions.length; i++)
				start_sessions_message[i+header.length] = start_sessions[i];
			checksum = calculate_checksum();
			bytearrmod(start_sessions_message, checksum, 14, 17);
			out.write(start_sessions_message);
			break ;
		}
		byte [] start_ack = new byte[52];
		while(true)
		{
			in.read(start_ack);
			extract(start_ack);
			receive();
			pause(100);
			display();
			System.out.println("Start ACK received.\n\n");
			break ;
		}
		for(glv = 0; glv < packets_sent; glv++)
		{
			send_test_packet();
			receive_test_packet();
		}
		System.out.println("Packets: Sent = " + packets_sent + ", Received = " + (packets_sent-packets_lost) + ", Lost = " + packets_lost + " (" + (packets_lost/packets_sent)*100.00 + "% loss)\n");
		System.out.println("Average Jitter = " + ((end_int*1000+end_frac/1000000)-(start_int*1000+start_frac/1000000))/(packets_sent-1) + " ms");
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
		bytearrmod(header, seq, 4, 7);
		bytearrmod(header, ack, 8, 11);
		for(int i = 0; i < header.length; i++)
			stop_sessions_command[i] = header[i];
		for(int i = 0; i < stop_sessions.length; i++)
			stop_sessions_command[i+header.length] = stop_sessions[i];
		checksum = calculate_checksum();
		bytearrmod(stop_sessions_command, checksum, 14, 17);
		out.write(stop_sessions_command);
		System.out.println("\n\n\nStopping current TWAMP session.\n\n");
		end();
	}
}