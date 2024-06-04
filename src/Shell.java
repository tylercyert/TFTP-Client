import java.io.File;
import java.io.IOException;

import javax.sound.sampled.*;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

public class Shell {

	private static Scanner scn = new Scanner(System.in);
	private static InetAddress inetAddress = null;
	static boolean connected = false;
	private static TFTPclient client = null;
	
	public static void main(String[] args) {
		
		boolean exitCond = false;
		String input = "";
		splash();
		//insert main method calls
		System.out.println("Welcome to TFTP client...\nType 'help' to see complete command list");
		
		
		//main command switch
		while(!exitCond){
			System.out.print(">");
			input = scn.nextLine().toLowerCase();
			switch(input){
			case "connect":
				connect();
				break;
			case "get": 
				get();
				break;
			case "put":
				put();
				break;
			case "help":
				help();
				break;
			case "exit":
				exitCond = true;
				break;
			default: 
				System.out.println("Invalid command!");
				break;
			}	
		}
		scn.close();
	}
	public static void connect() {
		inetAddress = getIP();
		String input = "";
		
		while (inetAddress == null) {
			inetAddress = getIP();
		}
		
		connected = true;
		
		System.out.println("Chose an option... (1 or 2)");
		System.out.println("1. Octet");
		System.out.println("2. netascii");
		
		while (!(input.equals("1") || input.equals("2"))) {
			input = scn.nextLine();
		}
		
		client = new TFTPclient(inetAddress.getHostAddress(), Integer.parseInt(input));
		System.out.println("Connection established");
	}
	
	public static void get() {
		boolean exitCond = false;
		String input = "";
		System.out.println("Enter a filename...");
		while (!exitCond){
			input = scn.nextLine();
			if (input == "") {
				exitCond = true;
			}
			else {
				try {
					client.rrq(input);
					exitCond = true;
					System.out.println("Transfer complete.");
				} catch (IOException e) {
					System.out.println("File not found please try again or press enter to quit.");
				}
			}
		}
		
		
	}
	public static void put() {
		boolean exitCond = false;
		String input;
		System.out.println("Enter a filename...");
		while (!exitCond){
			input = scn.nextLine();
			
			if (input == "") {
				exitCond = true;
			}
			else {
				try {
					client.wrq(input);
					exitCond = true;
					System.out.println("Transfer complete.");
				} catch (IOException e) {
					System.out.println("File not found please try again or press enter to quit.");
				}
			}
		}
		
		
	}
	public static void help() {
		System.out.println("Connect - Establish connection to TFTP server");
		System.out.println("Get - Retrieve file from TFTP server");
		System.out.println("Put - Transfer file to TFTP server");
		System.out.println("Help - You already know thos one!");
		System.out.println("Exit - Suspend this program");
	}
	public static void splash(){
		System.out.println("     /\\ ___________________________________________           ____     ____      /\\ ");
		System.out.println("    / / \\__    ___/\\_   _____/\\__    ___/\\______   \\         /_   |   /_   |    / / ");
		System.out.println("   / /    |    |    |    __)    |    |    |     ___/   ______ |   |    |   |   / /  ");
		System.out.println("  / /     |    |    |   |       |    |    |    |      /_____/ |   |    |   |  / /   ");
		System.out.println(" / /      |____|    \\___|       |____|    |____|              |___| /\\ |___| / /    ");
		System.out.println(" \\/                                                                 \\/       \\/    ");
		System.out.println("\t�2018 Tyler Cyert and Brendan Brevig");
		System.out.println("");
	}
	public static InetAddress getIP() {
		//Attempting to create a method to input IP address from console...
		InetAddress inetAddress = null;
		System.out.println("Please enter an IP address you would like to connect to...\nImportant: Make sure the address is correct and running a TFTP server...");
		Scanner scn = new Scanner(System.in);
		String ipText = scn.nextLine();
		try {
			inetAddress = InetAddress.getByName(ipText);
			connected = true;
		} catch (UnknownHostException e) {
			System.out.println("The IP you entered is either unavailable or invalid please try again!");
			inetAddress = null;
			connected = false;
		}

		return inetAddress;
	}

}
