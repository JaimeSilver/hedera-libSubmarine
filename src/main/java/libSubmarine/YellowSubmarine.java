package libSubmarine;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Random;

import com.hedera.accountWrappers.AccountCreate;
import com.hedera.sdk.common.HederaKeyPair;
import com.hedera.sdk.common.HederaKeyPair.KeyType;

import com.hedera.contractWrappers.ContractFunctionsWrapper;
import com.hedera.contractWrappers.ContractCreate;
import com.hedera.fileWrappers.FileCreate;
import com.hedera.utilities.ExampleUtilities;
import com.hedera.sdk.account.HederaAccount;
import com.hedera.sdk.common.HederaTransactionAndQueryDefaults;
import com.hedera.sdk.common.HederaTransactionRecord;
import com.hedera.sdk.common.Utilities;
import com.hedera.sdk.contract.HederaContract;
import com.hedera.sdk.file.HederaFile;

import org.bouncycastle.util.encoders.Hex;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.ByteUtil;

import libSubmarine.utils.Numeric;

public final class YellowSubmarine {
	static ContractFunctionsWrapper wrapper = new ContractFunctionsWrapper();

	public static void main(String... arguments) throws Exception {
		wrapper.setABIFromFile("./resources/thePoll.abi");

		// create a file and contract with contents
		boolean doFileContract = true;
		// validate Off-cain and On-chain SHA
		boolean doValidateSHA3 = false;
		// run and actual vote on the contract
		boolean runTheVote = true;

		// setup a set of defaults for query and transactions
		HederaTransactionAndQueryDefaults txQueryDefaults = ExampleUtilities.getTxQueryDefaults();
		txQueryDefaults.memo = "Submarine Load";

		// my account
		HederaAccount myAccount = new HederaAccount();

		// my account is going to generate all transactions.
		// But this is not a requirement (see Github README)
		myAccount.txQueryDefaults = txQueryDefaults;
		txQueryDefaults.fileWacl = txQueryDefaults.payingKeyPair;

		// setup the account number to process transactions
		myAccount.accountNum = txQueryDefaults.payingAccountID.accountNum;
		String userAddress = Utilities.calculateSolidityAddress(txQueryDefaults.payingAccountID.accountNum);

		HederaContract mainSubmarine = new HederaContract();
		long gas = 0;
		byte[] constructorData = wrapper.constructor(40); // Default 40 seconds for duration

		// create a file
		// new file object
		if (doFileContract) {
			HederaFile file = new HederaFile();
			file.txQueryDefaults = txQueryDefaults;
			byte[] fileContents = ExampleUtilities.readFile("./resources/thePoll.bin");
			ExampleUtilities.checkBinFile(fileContents);
			file = FileCreate.create(file, fileContents);

			// new contract object
			gas = 2000000;
			mainSubmarine.txQueryDefaults = txQueryDefaults;
			mainSubmarine = ContractCreate.create(mainSubmarine, file.getFileID(), gas, 0, constructorData);
			if (mainSubmarine != null) {
				mainSubmarine.getInfo();

				ExampleUtilities.showResult("@@@ The Submarine Contract is  " + mainSubmarine.contractNum);
			}
		} else {
			// Testnet as 20190729; referenced to speed time (if needed)
			mainSubmarine.contractNum = 36866;
		}

		/*
		 * This option shows the Off-chain and On-chain options of the LibSubmarine.
		 */
		byte[] dappData = new byte[1];
		byte[][] primaryKey = new byte[6][];
		byte[][] secondKey = new byte[3][];

		if (doValidateSHA3) {
			if (mainSubmarine.contractNum == 0) {
				return;
			}
			mainSubmarine.txQueryDefaults = txQueryDefaults;
			mainSubmarine.getInfo();

			String address_from = "ca35b7d915458ef540ade6068dfe2f44e8fa733c";
			String address_to = "1526613135cbe54ee257c11dd17254328a774f4a";
			long sendAmount = 60000;
			boolean verdad = true;
			String opcion = "ALF";

			/*
			 * The user can decide to use Randomizer or External String
			 */

			Random rand = new Random();
			long numb = Math.abs(rand.nextLong());
			String witness = Long.toString(numb);
			String witnessHex = Long.toHexString(numb);

			secondKey[0] = addressKeccak256(address_from); // Address From
			secondKey[1] = addressKeccak256(address_to); // Address To
			witness = "LA VACA LOLA";
			secondKey[2] = stringKeccak256(witness);// String
			String result1 = Hex.toHexString(HashUtil.sha3(ByteUtil.merge(secondKey)));
			
			// Generate Second Key on Hashgraph
			String hedera1 = wrapper.callLocalAddress(mainSubmarine, 10000000, 50000, "getSecondKey", address_from,
					address_to, witness);


			// Boolean option is not included in Smart Contract, but is enabled for future
			// reference
			//
			// primaryKey[6] = booleanKeccak256(verdad); // Boolean

			primaryKey[0] = addressKeccak256(address_from); // Address From
			primaryKey[1] = addressKeccak256(address_to); // Address To
			primaryKey[2] = longKeccak256(sendAmount); // Long
			String optionSelected = "ALF";
			primaryKey[3] = char32Keccak256(optionSelected); // Char32
			witness = "LA VACA LOLA";
			primaryKey[4] = stringKeccak256(witness);// String
			primaryKey[5] = dappData; // Bytes
			String result0 = Hex.toHexString(HashUtil.sha3(ByteUtil.merge(primaryKey)));

			// Generate First (main) Key on Hashgraph
			String hedera0 = wrapper.callLocalAddress(mainSubmarine, 10000000, 50000, "getSubmarineId", address_from,
					address_to, sendAmount, optionSelected, witness, dappData);

			//Show results on log
			ExampleUtilities.showResult("Local Witness        " + witness);
			ExampleUtilities.showResult("Local Secondary Key  " + result1);
			ExampleUtilities.showResult("Hedera Second Key    " + hedera1);

			ExampleUtilities.showResult("Local Submarine Key  " + result0);
			ExampleUtilities.showResult("Hedera Submarine Key " + hedera0);

		}
		
		//Declare variables for Actual run;
		String hedera0;
		String hedera1;
		String result0;
		String result1;
		Long voteNumber;

		if (runTheVote) {
			/*
			 * This example is executed with the same user Calling and Revealing, but the
			 * user can decide what Address (FROM) is the Reveal going to be invoked.
			 * 
			 * The default duration is set for 40 seconds, and during that time, no reads
			 * are allowed.
			 * 
			 * Workflow
			 * 
			 * 1) OFF-CHAIN: Using the functions provided, the user generates a Primary and
			 * Secondary Keys We have provided help methods in Solidity to help in this
			 * process, since it is obvious that the user will not pass the arguments to the
			 * network before the reveal time.
			 * 
			 * Get Primary Key: 
			 * ----> Input: Witnessing Address (address) 
			 * ----> Contract Address (address) 
			 * ----> Witness string to prove origin (string) 
			 * <---- Output: SecondaryKey in HEX
			 * 
			 * Get Secondary Key: 
			 * ----> Input: Witnessing Address (address) 
			 * ----> Contract Address (address) 
			 * ----> Commit Value (uint256) In case user wants to support payments
			 * ----> Option Selected (char32) Option selected for the polling options 
			 * ----> Witness string (Secret Phrase) (string) 
			 * ----> Embedded Data. Extension as suggested by libSubmarines 
			 * <---- Output: PrimaryKey in HEX
			 */

			if (mainSubmarine.contractNum == 0) {
				return;
			}
			mainSubmarine.txQueryDefaults = txQueryDefaults;
			mainSubmarine.getInfo();

			// userAddress is using the Paying Address;
			// user can decide the Address sender that will call the Reveal
			String submarineAddress = Utilities.calculateSolidityAddress(mainSubmarine.contractNum);

			primaryKey[0] = addressKeccak256(userAddress);
			primaryKey[1] = addressKeccak256(submarineAddress);
			primaryKey[2] = longKeccak256(0);
			String optionSelected = "ALF";
			primaryKey[3] = char32Keccak256(optionSelected);
			primaryKey[4] = stringKeccak256("LA VACA LOLA"); // Secret Phrase
			primaryKey[5] = dappData;
			result0 = Hex.toHexString(HashUtil.sha3(ByteUtil.merge(primaryKey)));
			
			/* 
			 * Clearly you will not send this to the Network BEFORE reveal
			 * it is provided for testing purposes.
			 */
			
			hedera0 = wrapper.callLocalAddress(mainSubmarine, 10000000, 50000, "getSubmarineId", userAddress,
					submarineAddress, 0, optionSelected, "LA VACA LOLA", dappData);
			ExampleUtilities.showResult("Local Submarine Key  " + result0);
			ExampleUtilities.showResult("Hedera Submarine Key " + hedera0);

			secondKey[0] = addressKeccak256(userAddress);
			secondKey[1] = addressKeccak256(submarineAddress);
			secondKey[2] = stringKeccak256("LA VACA LOLA");
			result1 = Hex.toHexString(HashUtil.sha3(ByteUtil.merge(secondKey)));

			/* 
			 * Clearly you will not send this to the Network BEFORE reveal
			 * it is provided for testing purposes.
			 */

			hedera1 = wrapper.callLocalAddress(mainSubmarine, 10000000, 50000, "getSecondKey", userAddress,
					submarineAddress, "LA VACA LOLA");
			ExampleUtilities.showResult("Local Secondary Key  " + result1);
			ExampleUtilities.showResult("Hedera Second Key    " + hedera1);

			/*
			 * Workflow
			 * 
			 * 2a) ON-CHAIN: The system can control voting times and reveal times. In this case
			 * they are the same
			 */

			Long voteReveal = wrapper.callLocalLong(mainSubmarine, 10000000, 50000, "minRevealTimestamp");
			Date date = new Date(voteReveal * 1000L);
			ExampleUtilities.showResult("Minimum Reveal Timestamp " + date.toString());

			Long voteRead = wrapper.callLocalLong(mainSubmarine, 10000000, 50000, "minReadTimestamp");
			date = new Date(voteRead * 1000L);
			ExampleUtilities.showResult("Minimum Read Timestamp   " + date.toString());

			/*
			 * Make sure that the options are in the Contract
			 */
			if (!wrapper.callLocalBoolean(mainSubmarine, 10000000, 50000, "isOption", "KERMIT")) {
				ExampleUtilities.showResult("*** Creating KERMIT in Contract");
				wrapper.callLong(mainSubmarine, 10000000, 0, "addOptions", "KERMIT");
			}
			if (!wrapper.callLocalBoolean(mainSubmarine, 10000000, 50000, "isOption", "ALF")) {
				ExampleUtilities.showResult("*** Creating ALF in Contract");
				wrapper.callLong(mainSubmarine, 10000000, 0, "addOptions", "ALF");
			}
			if (!wrapper.callLocalBoolean(mainSubmarine, 10000000, 50000, "isOption", "DORA")) {
				ExampleUtilities.showResult("*** Creating DORA in Contract");
				wrapper.callLong(mainSubmarine, 10000000, 0, "addOptions", "DORA");
			}

			/*
			 * Workflow
			 * 
			 * 2b) ON-CHAIN: Register the Primary and secondary keys
			 */
			ExampleUtilities.showResult("*** Casting the vote");
			wrapper.callLong(mainSubmarine, 10000000, 0, "registry", addressKeccak256(result0),
					addressKeccak256(result1));

			ExampleUtilities.showResult("*** countPoll   may revert if it is not time yet");
			try {
				// voteNumber = wrapper.callLocalLong(mainSubmarine, 10000000, 50000,
				// "countPoll");
				voteNumber = wrapper.callLong(mainSubmarine, 10000000, 0, "countPoll");
				ExampleUtilities.showResult("Number of Votes          " + Long.toString(voteNumber));
			} catch (Exception e) {
				ExampleUtilities.showResult("*** YEP failed!! ");
			}
			ExampleUtilities.showResult("*** Waiting 30 seconds");
			Thread.sleep(30000);

			/*
			 * Workflow
			 * 
			 * 3) ON-CHAIN: Address registered in the submarine sends the Reveal 
			 */
			ExampleUtilities.showResult("*** Revealing the vote");
			if (wrapper.callBoolean(mainSubmarine, 10000000, 0, "revealOption", addressKeccak256(result0), dappData,
					"LA VACA LOLA", 0)) {
				ExampleUtilities.showResult("*** Voting was revealed                          ");
				ExampleUtilities.showResult("*** Waiting for Consensus to replicate the State ");				
				Thread.sleep(3000);
			}
			
			/*
			 * Workflow
			 * 
			 * 4) ONLINE: Get the results NOTE: Hedera makes block.timestamp = fair order of
			 * the TX consensus; you need to submit CallFunctions, not CallLocal, to force an update 
			 * of the status of the State variables (e.g. block.timestamp).
			 */
			
			ExampleUtilities.showResult("*** Getting the vote");
			voteNumber = wrapper.callLong(mainSubmarine, 10000000, 0, "countPoll");
			ExampleUtilities.showResult("Number of Votes       " + Long.toString(voteNumber));
			Long votesALF = wrapper.callLong(mainSubmarine, 10000000, 0, "getTally", "ALF");
			ExampleUtilities.showResult("Votes for ALF " + Long.toString(votesALF));
			Long votesKERMIT = wrapper.callLong(mainSubmarine, 10000000, 0, "getTally", "KERMIT");
			ExampleUtilities.showResult("Votes for KERMIT " + Long.toString(votesKERMIT));
			Long votesDORA = wrapper.callLong(mainSubmarine, 10000000, 0, "getTally", "DORA");
			ExampleUtilities.showResult("Votes for DORA " + Long.toString(votesDORA));
		}
	}

	public static byte[] stringKeccak256(String value) {
		byte[] bytes = value.getBytes();
		return bytes;
	}

	public static byte[] char32Keccak256(String value) {
		byte[] ret = new byte[32];
		byte[] bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
		System.arraycopy(bytes, 0, ret, 0, bytes.length);
		return ret;
	}

	public static byte[] addressKeccak256(String address) {
		byte[] bytes = Numeric.hexStringToByteArray(address);
		return bytes;
	}

	public static byte[] longKeccak256(long longInput) {
		BigInteger bigInt = BigInteger.valueOf(longInput);
		byte[] bytes = ByteUtil.bigIntegerToBytesSigned(bigInt, 32);
		return bytes;
	}

	public static byte[] booleanKeccak256(boolean value) {
		byte[] bytes = new byte[1];
		bytes[0] = (byte) (value ? 1 : 0);
		return bytes;
	}

}