import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class server
{
	static Socket socket = null ;
	static ServerSocket server = null ;
	static DataInputStream in = null ;
	static DataOutputStream out = null ;
	static Random random = new Random();
	
	static Boolean URG, ACK, PSH, RST, SYN, FIN ;
	static int src_port, dest_port, seq, ack, HLEN, win_size, checksum, urg_ptr, temp ;
	static long OFFSET = 2208988800L ;
	
	static byte [] header = new byte[20];
	
	server(int port) throws IOException
	{
		start(port);
	}
	
	static int bytearr_to_int(byte arr[], int s, int e)
	{
		int res = 0 ;
		for(int i = s; i <= e; i++)
		{
			if((int)arr[i] >= 0)
				res = res*256 + (int)arr[i];
			else
				res = res*256 + (int)arr[i] + 256 ;
		}
		return res ;
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
	
	static void bytearrmod(byte [] arr, int val, int s, int e)
	{
		ByteBuffer bb = ByteBuffer.allocate(e-s+1);
		bb.putInt(val);
		byte[] temp = bb.array();
		for(int i = 0; i <= e-s; i++)
			arr[i+s] = temp[i];
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
	
	static void start(int p)
	{
		try
		{
			server = new ServerSocket(p);
			System.out.println("Waiting for client.");
			socket = server.accept();
			System.out.println("Client is online.\n\n");
			in = new DataInputStream(socket.getInputStream());
			out = new DataOutputStream(socket.getOutputStream());
			while(true)
			{
				in.read(header);
				if((header[13]&0x01) != 1)
				{
					receive();
					pause(1000);
					display();
					System.out.println("\n\n");
					if(SYN && !ACK)
					{
						ACK = true ;
						header[13] |= 0x10 ;
						temp = seq ;
						seq = ack ;
						ack = temp ;
						ack++ ;
						bytearrmod(header, seq, 4, 7);
						bytearrmod(header, ack, 8, 11);
						checksum = calculate_checksum();
						bytearrmod(header, checksum, 14, 17);
					}
					if(!SYN && ACK)
					{
						if(checksum == calculate_checksum())
						{
							pause(1000);
							System.out.println("TCP connection established.\n\n");
							PSH = true ;
							header[13] |= 0x18 ;
						}
						break ;
					}
					pause(1000);
					out.write(header);
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
				in.read(header);
				if(((header[13]&0x04) >> 2) != 1)
				{
					temp = seq ;
					seq = ack ;
					ack = temp ;
					ack++ ;
					bytearrmod(header, seq, 4, 7);
					bytearrmod(header, ack, 8, 11);
					receive();
					pause(1000);
					display();
					header[13] |= 0x11 ;
					checksum = calculate_checksum();
					bytearrmod(header, checksum, 14, 17);
					pause(1000);
					out.write(header);
					if(ACK)
					{
						in.close();
						out.close();
						System.out.println("TCP connection terminated.");
						socket.close();
						server.close();
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
	
	static void extract(byte msg[])
	{
		for(int i = 0; i < header.length; i++)
			header[i] = msg[i];
	}
	
	public static void main(String[] args) throws IOException
	{
		long temp2 = (long)(System.nanoTime()%1e9);
		long temp1 = (long)(System.currentTimeMillis()/1000)+OFFSET ;
		server s = new server(862);
		byte [] server_greeting = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,								// unused, so leave it
								   0x00, 0x00, 0x00, 0x01,																				// 1 for unauthenticated, 2 for authenticated, and 4 for encrypted
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// challenge, random number
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// salt, psuedo-random
								   0x00, 0x00, 0x04, 0x00,																				// count, must atleast be 1024
								   0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};								// MBZ
		byte [] challenge = new byte[16];
		byte [] salt = new byte[16];
		random.nextBytes(challenge);
		random.nextBytes(salt);
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
		bytearrmod(server_greeting_message, checksum, 14, 17);
		out.write(server_greeting_message);
		byte [] set_up_response = new byte[184];
		while(true)
		{
			in.read(set_up_response);
			extract(set_up_response);
			receive();
			pause(1000);
			display();
			System.out.println("Received set-up response.\n\n");
			break ;
		}
		byte [] server_start = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// MBZ (last octet being 0x00 implies no problem)
								0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,		// Server-IV (unused in unauthenticated mode)
								0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,														// timestamp of server start
								0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};													// MBZ
		// manually writing timestamp in bytes cause Java sucks, doesn't handle unsigned int
		server_start[32] = (byte)((temp1 & 0x00000000FF000000L) >> 24);
		server_start[33] = (byte)((temp1 & 0x0000000000FF0000L) >> 16);
		server_start[34] = (byte)((temp1 & 0x000000000000FF00L) >> 8);
		server_start[35] = (byte)((temp1 & 0x00000000000000FFL));
		server_start[36] = (byte)((temp2 & 0x00000000FF000000L) >> 24);
		server_start[37] = (byte)((temp2 & 0x0000000000FF0000L) >> 16);
		server_start[38] = (byte)((temp2 & 0x000000000000FF00L) >> 8);
		server_start[39] = (byte)((temp2 & 0x00000000000000FFL));
		byte [] server_start_message = new byte[header.length+server_start.length];
		temp = seq ;
		seq = ack ;
		ack = temp ;
		ack += (set_up_response.length-header.length);
		bytearrmod(header, seq, 4, 7);
		bytearrmod(header, ack, 8, 11);
		for(int i = 0; i < header.length; i++)
			server_start_message[i] = header[i];
		for(int i = 0; i < server_start.length; i++)
			server_start_message[i+header.length] = server_start[i];
		checksum = calculate_checksum();
		bytearrmod(server_start_message, checksum, 14, 17);
		out.write(server_start_message);
		byte [] request_session_message = new byte[148];
		while(true)
		{
			in.read(request_session_message);
			extract(request_session_message);
			receive();
			pause(1000);
			display();
			System.out.println("IP Version : IPv" + request_session_message[21]);
			System.out.println("Sender Port : " + bytearr_to_int(request_session_message, 32, 33));
			System.out.println("Receiver Port : " + bytearr_to_int(request_session_message, 34, 35));
			System.out.print("Sender Address : ");
			for(int i = 36; i < 39; i++)
				System.out.print(bytearr_to_int(request_session_message, i, i) + ".");	// I need to use a separate function just because Java doesn't support unsigned values smh
			System.out.println(bytearr_to_int(request_session_message, 39, 39));
			System.out.print("Receiver Address : ");
			for(int i = 52; i < 55; i++)
				System.out.print(bytearr_to_int(request_session_message, i, i) + ".");
			System.out.println(bytearr_to_int(request_session_message, 55, 55));
			long sst_int = 0, sst_frac = 0 ;
			for(int i = 88; i < 92; i++)
			{
				if((int)request_session_message[i] >= 0)
					sst_int = sst_int*256 + request_session_message[i];
				else
					sst_int = sst_int*256 + request_session_message[i]+256 ;
			}
			for(int i = 92; i < 96; i++)
			{
				if((int)request_session_message[i] >= 0)
					sst_frac = sst_frac*256 + request_session_message[i];
				else
					sst_frac = sst_frac*256 + request_session_message[i]+256 ;
			}
			Date date = new Date((sst_int-OFFSET)*1000+(sst_frac/(long)1e6));
			SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss z");
			sdf.setTimeZone(java.util.TimeZone.getTimeZone("GMT+5:30")); 
			System.out.println("Start Time : " + sdf.format(date).substring(0, sdf.format(date).length()-10) + "." + sst_frac + " IST");
			System.out.println("Received request session message.\n\n");
			byte [] accept_session = new byte[48];
			accept_session[0] = 0x00 ;	// Accept
			accept_session[1] = 0x00 ;	// MBZ
			for(int i = 2; i < 4; i++)
				accept_session[i] = request_session_message[i+30];	// Port number
			for(int i = 4; i < 8; i++)
				accept_session[i] = request_session_message[i+48];	// IP of generating machine
			temp2 = (long)(System.nanoTime()%1e9);
			temp1 = (long)(System.currentTimeMillis()/1000)+OFFSET ;
			// still disappointed over the fact that Java doesn't handle unsigned values
			accept_session[8] = (byte)((temp1 & 0x00000000FF000000L) >> 24);
			accept_session[9] = (byte)((temp1 & 0x0000000000FF0000L) >> 16);
			accept_session[10] = (byte)((temp1 & 0x000000000000FF00L) >> 8);
			accept_session[11] = (byte)((temp1 & 0x00000000000000FFL));
			accept_session[12] = (byte)((temp2 & 0x00000000FF000000L) >> 24);
			accept_session[13] = (byte)((temp2 & 0x0000000000FF0000L) >> 16);
			accept_session[14] = (byte)((temp2 & 0x000000000000FF00L) >> 8);
			accept_session[15] = (byte)((temp2 & 0x00000000000000FFL));
			byte [] r = new byte[4];
			random.nextBytes(r);
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
			bytearrmod(header, seq, 4, 7);
			bytearrmod(header, ack, 8, 11);
			for(int i = 0; i < header.length; i++)
				accept_session_message[i] = header[i];
			for(int i = 0; i < accept_session.length; i++)
				accept_session_message[i+header.length] = accept_session[i];
			checksum = calculate_checksum();
			bytearrmod(accept_session_message, checksum, 14, 17);
			out.write(accept_session_message);
			break ;
		}
		byte [] start_sessions_message = new byte[52];
		while(true)
		{
			in.read(start_sessions_message);
			extract(start_sessions_message);
			receive();
			pause(1000);
			display();
			System.out.println("Start sessions message received.\n\n");
			byte [] start_ack = new byte[start_sessions_message.length];
			temp = seq ;
			seq = ack ;
			ack = temp ;
			ack += (start_sessions_message.length-header.length);
			bytearrmod(header, seq, 4, 7);
			bytearrmod(header, ack, 8, 11);
			for(int i = 0; i < header.length; i++)
				start_ack[i] = header[i];
			for(int i = 0; i < start_sessions_message.length-header.length; i++)
				start_ack[i+header.length] = start_sessions_message[i+header.length];
			start_ack[20] = 0x00 ;
			checksum = calculate_checksum();
			bytearrmod(start_ack, checksum, 14, 17);
			out.write(start_ack);
			break ;
		}
	}
}