import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.Random;

public class client
{
	static Socket socket = null;
	static DataInputStream in = null;
	static DataOutputStream out = null;
	static Scanner sc = new Scanner(System.in);
	static Random random = new Random();
	
	static Boolean URG = false, ACK = false, PSH = false, RST = false, SYN = true, FIN = false ;
	static int src_port = 862, dest_port = 862, seq = 0, ack = 0, HLEN = 5, win_size = 0, checksum = 0, urg_ptr = 0, TIME_WAIT = 5000, temp ;
	
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
					pause(1000);
					display();
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
						pause(1000);
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
			header[13] &= ~0x10 ;
			bytearrmod(header, seq, 4, 7);
			bytearrmod(header, ack, 8, 11);
			checksum = calculate_checksum();
			bytearrmod(header, checksum, 14, 17);
			pause(1000);
			out.write(header);
			while(true)
			{
				in.read(header);
				if(((header[13]&0x04) >> 2) != 1)
				{
					receive();
					pause(1000);
					display();
					if(ACK)
					{
						swap(seq, ack);
						ack++ ;
						bytearrmod(header, seq, 4, 7);
						bytearrmod(header, ack, 8, 11);
						FIN = false ;
						header[13] &= ~0x01 ;
						checksum = calculate_checksum();
						bytearrmod(header, checksum, 14, 17);
						out.write(header);
						System.out.println("Terminating TCP connection.\n");
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
			int j = sc.nextInt();
		}
	}
	
	static void extract(byte msg[])
	{
		for(int i = 0; i < header.length; i++)
			header[i] = msg[i];
	}
	
	static void swap(int a, int b)
	{
		temp = a ;
		a = b ;
		b = temp ;
	}
	
	public static void main(String[] args)
	{
		client c = new client("192.168.56.1", 862);
		byte [] server_greeting_message = new byte[84];
		while(true)
		{
			try
			{
				in.read(server_greeting_message);
				server_greeting_message[13] &= ~0x10 ;
				extract(server_greeting_message);
				receive();
				pause(1000);
				display();
				break ;
			}
			catch(IOException ioe)
			{
				System.out.println("\nServer has disconnected.\n\n");
			}
		}
		int i = sc.nextInt();
	}
}