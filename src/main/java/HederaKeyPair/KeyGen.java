package HederaKeyPair;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public final class KeyGen {

	private final static void printStars() {
		System.out.println("************************************************************************");
	}
	
	public static String[] generate ( String[] args ) {
		final String ED25519 = "ED25519";
		String[] response = new String[2];
		
		String keyType = "";
		String seed = "";
		List<String> recoveryWords = new ArrayList<String>();
		
		// command line input parameters
		if (args.length == 0) {
			// default to ED25519
			keyType = ED25519;
		} else if (args.length == 1) {
			// seed
			keyType = ED25519;
			seed = args[0];
			if (seed.length() != 32) {
				System.out.println("Seed length must be 32 bytes for ED25519");
				System.exit(3);
			}
		} else if (args.length == 22) {
			for (int i=0; i < 22; i++) {
				recoveryWords.add(args[i].replace(",", ""));
			}
		} else {
			System.out.println("Invalid input parameters");
			System.out.println("Should be");
			System.out.println("- no parameters - generates an ED25519 key");
			System.out.println("- a 32 byte seed string");
			System.out.println("- 22 recovery words separated by spaces");
			System.exit(4);
		}
		
		byte[] seedBytes = null;
		
		if (recoveryWords.size() == 22) {
			// recover key from words
			try {
				CryptoKeyPair keyPair = new CryptoKeyPair(recoveryWords);
				printStars();
				System.out.println("Your recovered key pair is:");
				System.out.print("Public key:");
				System.out.println(keyPair.getPublicKeyEncodedHex());
				response[0] = keyPair.getPublicKeyEncodedHex();
				System.out.print("Secret key:");
				System.out.println(keyPair.getSecretKeyHex());
				response[1] = keyPair.getSecretKeyHex();
				printStars();
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		} else {
			// generating new key
			if (!seed.equals("")) {
				// build up the seed byte array
				seedBytes = seed.getBytes();
			}
			CryptoKeyPair keyPair = new CryptoKeyPair(seedBytes);
			printStars();
			System.out.println("Your key pair is:");
			System.out.print("Public key:");
			System.out.println(keyPair.getPublicKeyEncodedHex());
			response[0] = keyPair.getPublicKeyEncodedHex();
			System.out.print("Secret key:");
			System.out.println(keyPair.getSecretKeyHex());
			response[1] = keyPair.getSecretKeyHex();
			System.out.println("Recovery word list:");
			System.out.println(keyPair.recoveryWordsList());
			printStars();
		}
		return response;
	}
}
