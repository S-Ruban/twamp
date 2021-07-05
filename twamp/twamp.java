package twamp;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.Scanner;

public class twamp
{
	public static final char[] HEX_ARRAY = "0123456789abcdef".toCharArray();
	public static Random random = new Random();
	public static long OFFSET = 2208988800L, temp3 ;
	public static Scanner sc = new Scanner(System.in);
	public static Date date = new Date(0);
	public static SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	
	public static Socket socket = null ;
	public static DataInputStream in = null ;
	public static DataOutputStream out = null ;
	public static ServerSocket server = null ;
	
	public static void bytearrmod(byte [] arr, int val, int s, int e)
	{
		ByteBuffer bb = ByteBuffer.allocate(e-s+1);
		bb.putInt(val);
		byte[] temp = bb.array();
		for(int i = 0; i <= e-s; i++)
			arr[i+s] = temp[i];
	}
	
	public static void bytearrmod(byte [] arr, short val, int s, int e)
	{
		ByteBuffer bb = ByteBuffer.allocate(e-s+1);
		bb.putShort(val);
		byte[] temp = bb.array();
		for(int i = 0; i <= e-s; i++)
			arr[i+s] = temp[i];
	}
	
	public static void bytearrmod(byte [] arr, long val, int s, int e)
	{
		ByteBuffer bb = ByteBuffer.allocate(e-s+1);
		bb.putLong(val);
		byte[] temp = bb.array();
		for(int i = 0; i <= e-s; i++)
			arr[i+s] = temp[i];
	}
	
	public static int bytearr_to_int(byte arr[], int s, int e)
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
	
	public static long bytearr_to_long(byte arr[], int s, int e)
	{
		long res = 0 ;
		for(int i = s; i <= e; i++)
		{
			if((int)arr[i] >= 0)
				res = res*256 + (int)arr[i];
			else
				res = res*256 + (int)arr[i] + 256 ;
		}
		return res ;
	}
	
	public static void long_to_bytearr(long val, byte [] arr, int s, int e)
	{
		arr[s] = (byte)((val & 0x00000000ff000000L) >> 24);
		arr[s+1] = (byte)((val & 0x0000000000ff0000L) >> 16);
		arr[s+2] = (byte)((val & 0x000000000000ff00L) >> 8);
		arr[s+3] = (byte)((val & 0x00000000000000ffL));
	}
	
	public static void int_to_bytearr(int val, byte [] arr, int s, int e)
	{
		arr[s] = (byte)((val & 0x0000ff00) >> 8);
		arr[s+1] = (byte)((val & 0x000000ff));
	}
	
	public static void pause(int ms)
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
}
