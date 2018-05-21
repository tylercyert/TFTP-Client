import java.io.*;
import java.net.*;
import java.util.*;

public class TFTPclient {
	static DatagramSocket dataSocket = null;
	static DatagramPacket dataPacketIn;
	static DatagramPacket dataPacketOut;

	public final static int ByteArraySize = 512;
	static byte[] sendBuffer = new byte[ByteArraySize];
	static byte[] receiveBuffer = new byte[ByteArraySize];

	//TFTP OP Codes
	static final byte opRRQ = 1;
	static final byte opWRQ = 2;
	static final byte opDATA = 3;
	static final byte opACK = 4;
	static final byte opERROR = 5;
	static final byte byteFill = 0;

	// Previously used to set IP to connect to and file to put or get from.
	// static String fileName = "";
	// private static final String serverIP = "10.19.109.234";
	private static String serverIP = "";
	public static final int tftpPort = 69;
	
	//testPort utilized after dedicated socket port set after request to connect in both rrq and wrq.
	static int testPort;
	private static InetAddress inetAddress = null;
	private static String connectType = null;
	
	//Type of mode used - NETASCII OR OCTET
	//do not believe this is being utilized by us correctly in this client.
	public TFTPclient(String ip, int num) {
		serverIP = ip;
		if (num == 2) {
			connectType = "netascii";
		} else {
			connectType = "octet";
		}
	}

	// Ignore connectRequest and receiveRequest: Blooper
	// used to test and gain basic understanding of sockets.
	public static void connectRequest() throws IOException {
		dataSocket = new DatagramSocket();
		inetAddress = InetAddress.getByName(serverIP);
		dataPacketOut = new DatagramPacket(sendBuffer, sendBuffer.length, inetAddress, tftpPort);
		dataSocket.send(dataPacketOut);
	}
	public static void receiveRequest() throws IOException {
		dataPacketIn = new DatagramPacket(receiveBuffer, receiveBuffer.length, inetAddress, dataSocket.getLocalPort());
		dataSocket.receive(dataPacketIn);
	}

	public void rrq(String fileToRead) throws IOException{
		// Initialize at start to clear if previously used
		byte[] sendRequestArray;
		DatagramPacket outDatagramPacket;
		ByteArrayOutputStream arrayOutputStream;

		// create rrq and and send to TFTP server listening on port 69.
		try {
			inetAddress = InetAddress.getByName(serverIP);
			dataSocket = new DatagramSocket();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Either Host Error or Socket Error - RRQ");
		}
	
		sendRequestArray = createRrqWrqPacket(opRRQ, fileToRead, connectType);
		outDatagramPacket = new DatagramPacket(sendRequestArray, sendRequestArray.length, inetAddress, tftpPort);
		try {
			dataSocket.send(outDatagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to send outDatagramPacket - RRQ request");
		}

		// call of outStream method which receives the data of the file transfer stores
		// in stream
		// and to eventually be written to file.
		arrayOutputStream = outStream();

		// write arrayOutputStream content to fileName
		writeFile(arrayOutputStream, fileToRead);

		// closes
		arrayOutputStream.close();
		dataSocket.close();
	}

	public static ByteArrayOutputStream outStream() throws IOException {
		// block number initialized at one
		int block = 1;
		ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
		do {
			// allocate new ByteBufferArray to keep empty.
			byte[] ByteBufferArray = new byte[ByteArraySize + 4];
			dataPacketIn = new DatagramPacket(ByteBufferArray, ByteBufferArray.length, inetAddress,
					dataSocket.getLocalPort());
			dataSocket.receive(dataPacketIn);
			
			//fill op byte array with first to elements of received ACK packet
			byte[] OP = { ByteBufferArray[0], ByteBufferArray[1] };

			// check opcode received and only if is DATA opCode
			if (OP[1] == opDATA) {
				// continue get block number, store in getBlock byte array
				byte getBlock[] = { ByteBufferArray[2], ByteBufferArray[3] };
				
				//check to see if the block number is the same as received
				//if the same, data packets still in order.
				//toInt converts byte to int.
				if (block == (toInt(getBlock))) {
					//block > 65535, 16bit signed avoidance
					if (block > 65535) {
						block = 0;
					}
						System.out.println("Data Packet: " + block + "Succsessfully Received");
						DataOutputStream OutputStreamWrite = new DataOutputStream(arrayOutputStream);
						OutputStreamWrite.write(dataPacketIn.getData(), (byte) 4, dataPacketIn.getLength() - (byte) 4);
						AckPacket(getBlock);
					}
				
					 //If block number is not the same as received, assumed packet loss and re-send previous ACK.
				  	 //decrement block to keep block correct, if re-sent.
					if (block != (toInt(getBlock))){
						System.out.println("Block received out of order, Resending Ack.");
						AckPacket(getBlock);
						block--;
					}
					block++;
					} 
					//If OP code received in DATA packet is anything BUT the DATA OP code
					//send "Invalid OP code received" OR if OP code is for an error, check the 
					//error code packet.
					else if (OP[1] == opERROR) {
						checkErrorCode(ByteBufferArray[3]);
						break;
					} else if (OP[1] == opRRQ) {
						ErrorPacket(opERROR, "Invalid OP code received");
						break;
					} else if (OP[1] == opWRQ) {
						ErrorPacket(opERROR, "Invalid OP code received");
						break;
					} else if (OP[1] == opACK) {
						ErrorPacket(opERROR, "Invalid OP code received");
						break;
					}

		// Exit loop when length of dataPacket RECEIVED is less than 512.
		}while(!(dataPacketIn.getLength()<512));

	return arrayOutputStream;
	}

	public void wrq(String fileToRead) throws IOException {
		DatagramPacket outDatagramPacket;
		DatagramPacket inDatagramPacket;
		byte[] sendRequestArray;

		// Create WRQ request packet and send to server on port 69.
		
		try {
			inetAddress = InetAddress.getByName(serverIP);
			dataSocket = new DatagramSocket();
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Either Host Error or Socket Error - WRQ");
		}
		sendRequestArray = createRrqWrqPacket(opWRQ, fileToRead, connectType);
		outDatagramPacket = new DatagramPacket(sendRequestArray, sendRequestArray.length, inetAddress, tftpPort);
		//try to send wrq request to server
		try {
			dataSocket.send(outDatagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to send outDatagramPacket - WRQ");
		}

		// receive (hopefully)ACK packet from server, check opcode, if block 0 good else
		// not.
		byte[] getAckBuffer = new byte[4];
		inDatagramPacket = new DatagramPacket(getAckBuffer, getAckBuffer.length, inetAddress,
				dataSocket.getLocalPort());
		//try to receive ACK packet from server
		try {
			dataSocket.receive(inDatagramPacket);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to received inDatagramPacket - WRQ");
		}

		// testPort will be port used to send on through rest of the transfer.
		testPort = inDatagramPacket.getPort();
		
		// check OP code.
		byte[] OP = { getAckBuffer[0], getAckBuffer[1] };

		if (OP[1] == opACK) {
			// code used from Mkyoung.com
			//'https://www.mkyong.com/java/how-to-convert-file-into-an-array-of-bytes/'
			File file = new File(fileToRead);
			int lengthOfFile = ((int) file.length());
			byte[] lengthBytes = new byte[((int) file.length())];
			FileInputStream fileIn = new FileInputStream(file);
			
			fileIn.read(lengthBytes);

			// call sendData which takes an byteArray of the WHOLE file
			sendData(lengthBytes);
			fileIn.close();
		} else {
			byte[] errorArray;
			errorArray = createErrorPacket(opERROR, "Invalid OP code received");
			dataPacketOut = new DatagramPacket(errorArray, errorArray.length, inetAddress, dataSocket.getLocalPort());
			dataSocket.send(dataPacketOut);
		}

		// close
		dataSocket.close();
	}

	public static void sendData(byte[] data) {
		// block starts at 0, first received ACK from server will be 0
		int block = 0;
		// Start is a counter utilized in copyOfRange calls to take 512 bytes from files
		// at a time.
		int start = 0;
		// while loop condition to continue.
		boolean out = false;
		byte[] blockToSend;

		while (!out) {
			block++;
			
			//while the total data to send minus the data sent is still greater than 512 bytes.
			//use copyOfRange to get and send the block of 512 bytes
			if (data.length - start > ByteArraySize) {
				blockToSend = Arrays.copyOfRange(data, start, start + ByteArraySize);
				start = (start + ByteArraySize);
				
			//ELSE the amount of bytes to send is less than 512, indicating the last block to send.
			} else {
				blockToSend = Arrays.copyOfRange(data, start, data.length);
				out = true;
			}

			try {
				blockToSend = createDataPacket(block, blockToSend);
			} catch (IOException e) {
				System.out.print("error creating data packet");
			}

			try {
				dataPacketOut = new DatagramPacket(blockToSend, blockToSend.length, inetAddress, testPort);
				dataSocket.send(dataPacketOut);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Failed to create or send dataPacketOut - sendData");
			}

			// byte array of 4 to receive ack packet from server.
			byte[] ackReceive = new byte[4];

			// try to receive ack packet
			try {
				dataPacketIn = new DatagramPacket(ackReceive, ackReceive.length, inetAddress, testPort);
				dataSocket.receive(dataPacketIn);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Error creating or receiving ack packet - sendData");
			}

			// CHECK IF OPCODE RECEIVED WAS ACK OR ERROR!
			if (ackReceive[1] == opERROR) {
				checkErrorCode(ackReceive[3]);
				break;
			}
		}
	}

	// CREATION OF DATA PACKET DONE FOR WRITE REQUEST.
	public static byte[] createDataPacket(int intBlock, byte[] blockData) throws IOException {

		byte[] numBlock = new byte[2];

		// Cite where this was taken from.
		numBlock[0] = (byte) (intBlock & 0xFF);
		numBlock[1] = (byte) ((intBlock >> 8) & 0xFF);
		ByteArrayOutputStream dataSending = new ByteArrayOutputStream();
		// Data Packet format.
		dataSending.write(byteFill);
		dataSending.write(opDATA);
		dataSending.write(numBlock[1]);
		dataSending.write(numBlock[0]);
		dataSending.write(blockData);

		// returns the byteArray of the output stream dataSending
		return dataSending.toByteArray();
	}

	//code taken from stack overflow.. 'magic' 
	//'https://stackoverflow.com/questions/1936857/convert-integer-into-byte-array-java'
	public static int toInt(byte[] blockNum) {
		// return blockNum[0] << 24 | (blockNum[1] & 0xFF) << 16;
		return blockNum[0] & 0xff << 8 | blockNum[1] & 0xff;
	}

	// Method creates ackPacket with block number and sends to server.
	public static void AckPacket(byte[] numBlock) throws UnknownHostException {
		byte[] ackArray = { byteFill, opACK, numBlock[0], numBlock[1] };
		inetAddress = InetAddress.getByName(serverIP);
		dataPacketOut = new DatagramPacket(ackArray, ackArray.length, inetAddress, dataPacketIn.getPort());
		try {
			dataSocket.send(dataPacketOut);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to send dataPacketOut - AckPacket");
		}
	}

	public static void ErrorPacket(byte opError, String errorString) throws UnknownHostException {
		byte[] errorArray = createErrorPacket(opError, errorString);
		inetAddress = InetAddress.getByName(serverIP);
		dataPacketOut = new DatagramPacket(errorArray, errorArray.length, inetAddress, dataPacketIn.getPort());
		try {
			dataSocket.send(dataPacketOut);
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Failed to send dataPacketOut - ErrorPacket");
		}
	}
	
	// NEED TO CREATE ERROR PACKET TO TRY TO SEND.
	public static byte[] createErrorPacket(byte errorCode, String errMessage) {
		int stringLength = errMessage.length();
		int errorPackSize = stringLength + 5;
		byte[] errorPacket = new byte[errorPackSize];

		errorPacket[0] = byteFill;
		errorPacket[1] = opERROR;
		errorPacket[2] = byteFill;
		errorPacket[3] = errorCode;
		int count = 4;

		for (int i = 0; i < stringLength; i++) {
			errorPacket[count] = (byte) errMessage.charAt(i);
			count++;
		}
		errorPacket[count] = byteFill;
		return errorPacket;

	}

	public static void checkErrorCode(byte errorCode) {
		switch (errorCode) {
		case 0:
			System.out.println("Not defined, see error message (if any)");
			break;
		case 1:
			System.out.println("File not found.");
			break;
		case 2:
			System.out.println("Access violation.");
			break;
		case 3:
			System.out.println("Disk full or allocation exceeded.");
			break;
		case 4:
			System.out.println("Illegal TFTP operation.");
			break;
		case 5:
			System.out.println("Unkown transfer ID.");
			break;
		case 6:
			System.out.println("File already exists.");
			break;
		case 7:
			System.out.println("No such user.");
			break;
		}
	}

	// Creates either Rrq or Wrq packet based on which opcode passed, and utilizing
	// whichever
	// fileName and which ever mode of data - OCTET ASCII
	public static byte[] createRrqWrqPacket(byte opCode, String fileName, String Mode) {
		int fileLength = fileName.length();
		int modeLength = Mode.length();
		int byteArrayLength = 2 + fileLength + 1 + modeLength + 1;
		byte[] RrqWrq = new byte[byteArrayLength];

		RrqWrq[0] = byteFill;
		RrqWrq[1] = opCode;
		int count = 2;
		for (int i = 0; i < fileLength; i++) {
			RrqWrq[count] = (byte) fileName.charAt(i);
			count++;
		}
		RrqWrq[count] = byteFill;
		count++;
		for (int i = 0; i < modeLength; i++) {
			RrqWrq[count] = (byte) Mode.charAt(i);
			count++;
		}
		RrqWrq[count] = byteFill;
		return RrqWrq;

	}

	// Write to outputStream FILE
	public static void writeFile(ByteArrayOutputStream outStreamWrite, String fileNameWrite) throws IOException {
		OutputStream outputStream = new FileOutputStream(fileNameWrite);
		outStreamWrite.writeTo(outputStream);
	}
}
