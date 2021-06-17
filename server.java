import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.Random;

public class server
{
	static Socket socket = null ;
	static ServerSocket server = null ;
	static DataInputStream in = null ;
	static DataOutputStream out = null ;
	static Random random = new Random();
	static Scanner sc = new Scanner(System.in);
	
	static Boolean URG, ACK, PSH, RST, SYN, FIN ;
	static int src_port, dest_port, seq, ack, HLEN, win_size, checksum, urg_ptr, temp ;
	
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
			if((int)header[i] >= 0)
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
		System.out.println("Checksum : " + checksum);
		System.out.println("Urgent pointer : " + urg_ptr);
		if(checksum == calculate_checksum())
			System.out.println("Checksums match.\n\n");
		else
			System.out.println("Checksums do not match.\n\n");
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
					if(SYN && !ACK)
					{
						ACK = true ;
						header[13] |= 0x10 ;
						swap(seq, ack);
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
						}
						ACK = false ;
						header[13] &= ~0x10 ;
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
					swap(seq, ack);
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
	
	static void swap(int a, int b)
	{
		temp = a ;
		a = b ;
		b = temp ;
	}
	
	public static void main(String[] args) throws IOException
	{
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
		header[13] &= ~0x10 ;
		checksum = calculate_checksum();
		for(int i = 0; i < header.length; i++)
			server_greeting_message[i] = header[i];
		for(int i = 0; i < server_greeting.length; i++)
			server_greeting_message[header.length+i] = server_greeting[i];
		server_greeting_message[13] &= ~0x10 ;
		checksum = calculate_checksum();
		bytearrmod(server_greeting_message, checksum, 14, 17);
		out.write(server_greeting_message);
		int i = sc.nextInt();
	}
}