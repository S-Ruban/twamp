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
	Socket socket = null ;
	ServerSocket server = null ;
	DataInputStream in = null ;
	DataOutputStream out = null ;
	Random rand = new Random();
	static Scanner sc = new Scanner(System.in);
	
	Boolean URG, ACK, PSH, RST, SYN, FIN ;
	int src_port, dest_port, seq, ack, HLEN, win_size, checksum, urg_ptr ;
	
	byte [] header = new byte[20];
	
	server(int port) throws IOException
	{
		try
		{
			server = new ServerSocket(port);
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
						ack = seq + 1 ;
						seq = rand.nextInt(Integer.MAX_VALUE);
						header_mod(seq, 4, 7);
						header_mod(ack, 8, 11);
						checksum = calculate_checksum();
						header_mod(checksum, 14, 17);
					}
					if(!SYN && ACK)
					{
						ACK = false ;
						if(checksum == calculate_checksum())
							System.out.println("TCP connection established.");
						header[13] &= ~0x10 ;
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
	
	int bytearr_to_int(int s, int e)
	{
		int res = 0 ;
		for(int i = s; i <= e; i++)
		{
			if((int)header[i] >= 0)
				res = res*256 + (int)header[i];
			else
				res = res*256 + (int)header[i] + 256 ;
		}
		return res ;
	}
	
	void receive()
	{
		src_port = bytearr_to_int(0,1);
		dest_port = bytearr_to_int(2,3);
		seq = bytearr_to_int(4,7);
		ack = bytearr_to_int(8,11);
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
		win_size = bytearr_to_int(14,15);
		checksum = bytearr_to_int(16,17);
		urg_ptr = bytearr_to_int(18,19);
	}
	
	int calculate_checksum()
	{
		int cs = 0 ;
		for(int x = 0; x < 20; x += 2)
		{
			if(x == 16)
				continue ;
			else
				cs += (65535-bytearr_to_int(x, x+1));
		}
		cs = (~cs) & 0xffff ;
		return cs ;
	}
	
	void header_mod(int val, int s, int e)
	{
		ByteBuffer bb = ByteBuffer.allocate(e-s+1);
		bb.putInt(val);
		byte[] temp = bb.array();
		for(int i = 0; i <= e-s; i++)
			header[i+s] = temp[i];
	}
	
	void display()
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
	
	void pause(int ms)
	{
		try
		{
			Thread.sleep(ms);
		}
		catch(InterruptedException iioe)
		{
			System.out.println(iioe);
		}
	}
	
	public static void main(String[] args) throws IOException
	{
		new server(832);
		int i = sc.nextInt();
	}
}