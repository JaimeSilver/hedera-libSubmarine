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

public final class GuessingGame {
	static ContractFunctionsWrapper wrapperRunner = new ContractFunctionsWrapper();
	static ContractFunctionsWrapper wrapperSubmarine = new ContractFunctionsWrapper();

	public static void main(String... arguments) throws Exception {
		wrapperRunner.setABIFromFile("./resources/theRunnerContract.abi");
		wrapperSubmarine.setABIFromFile("./resources/theGuessingGame.abi");

		// create a file and contract with contents
		boolean doFileContract = false;
		// run and actual game
		boolean runTheGame = true;

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

		HederaContract mainRunner = new HederaContract();
		HederaContract mainSubmarine = new HederaContract();
		long gas = 0;

		// create a file
		// new file object
		if (doFileContract) {
			HederaFile file = new HederaFile();
			file.txQueryDefaults = txQueryDefaults;
			byte[] fileContents = ExampleUtilities.readFile("./resources/theRunnerContract.bin");
			ExampleUtilities.checkBinFile(fileContents);
			file = FileCreate.create(file, fileContents);

			// new contract object
			gas = 2000000;
			mainRunner.txQueryDefaults = txQueryDefaults;
			mainRunner = ContractCreate.create(mainRunner, file.getFileID(), gas, 0);
			if (mainRunner != null) {
				mainRunner.getInfo();
				ExampleUtilities.showResult("@@@ The Runner Contract is  " + mainRunner.contractNum);
				String submarineAddress = wrapperRunner.callLocalAddress(mainRunner, 1000000, 50000,
						"getSubmarineAddress");
				mainSubmarine = readContract(submarineAddress);
				ExampleUtilities.showResult("@@@ The Submarine Contract is  " + mainSubmarine.contractNum);

			}
		} else {
			// Testnet as 20190729; referenced to speed time (if needed)
			mainRunner.contractNum = 36934;
			mainSubmarine.contractNum = 36935;
		}

		/*
		 * This option shows the Off-chain and On-chain options of the LibSubmarine.
		 */
		byte[] dappData = new byte[1];
		byte[][] primaryKey = new byte[6][];
		byte[][] secondKey = new byte[3][];
		String hedera0;
		String hedera1;
		String result0;
		String result1;
		Long gameNumber;

		if (runTheGame) {

			if (mainSubmarine.contractNum == 0 || mainRunner.contractNum == 0) {
				return;
			}
			mainSubmarine.txQueryDefaults = txQueryDefaults;
			mainSubmarine.getInfo();
			mainRunner.txQueryDefaults = txQueryDefaults;
			mainRunner.getInfo();

			// userAddress is using the Paying Address;
			// the functions will always be invoked from the runnerAddress
			String runnerAddress = Utilities.calculateSolidityAddress(mainRunner.contractNum);
			String submarineAddress = Utilities.calculateSolidityAddress(mainSubmarine.contractNum);

			primaryKey[0] = addressKeccak256(runnerAddress);
			primaryKey[1] = addressKeccak256(submarineAddress);

			// Defining the Reward 5 Hbars
			long bounty = 500000000;
			primaryKey[2] = longKeccak256(bounty);
			String optionSelected = "";
			primaryKey[3] = char32Keccak256(optionSelected);

			Random rand = new Random();
			long numb = Math.abs(rand.nextLong());
			long winner = ((numb) / 100) % 10;
			ExampleUtilities.showResult("The number that should reveal the puzzle is " + Long.toString(winner));			
			String witness = Long.toString(winner);

			primaryKey[4] = stringKeccak256(witness); // Digit from 0 to 9
			primaryKey[5] = dappData;
			result0 = Hex.toHexString(HashUtil.sha3(ByteUtil.merge(primaryKey)));

			/*
			 * Clearly you will not send this to the Network BEFORE reveal it is provided
			 * for testing purposes.
			 */

			hedera0 = wrapperSubmarine.callLocalAddress(mainSubmarine, 10000000, 50000, "getSubmarineId", runnerAddress,
					submarineAddress, bounty, optionSelected, witness, dappData);
			ExampleUtilities.showResult("Local Submarine Key  " + result0);
			ExampleUtilities.showResult("Hedera Submarine Key " + hedera0);

			secondKey[0] = addressKeccak256(userAddress);
			secondKey[1] = addressKeccak256(submarineAddress);
			secondKey[2] = stringKeccak256(witness);
			result1 = Hex.toHexString(HashUtil.sha3(ByteUtil.merge(secondKey)));

			/*
			 * Clearly you will not send this to the Network BEFORE reveal it is provided
			 * for testing purposes.
			 */

			hedera1 = wrapperSubmarine.callLocalAddress(mainSubmarine, 10000000, 50000, "getSecondKey", userAddress,
					submarineAddress, witness);
			ExampleUtilities.showResult("Local Secondary Key  " + result1);
			ExampleUtilities.showResult("Hedera Second Key    " + hedera1);

			/*
			 * Workflow
			 * 
			 * 2a) ON-CHAIN: Submit a game number with the two keys; Have to deposit 5 Hbars
			 * to start the game
			 */

			/*
			 * Make sure that the options are in the Contract
			 */
			gameNumber = numb;
			if (!wrapperRunner.callLocalBoolean(mainRunner, 10000000, 50000, "isGame", gameNumber)) {
				ExampleUtilities.showResult("*** Creating Game Number & submarine "+ Long.toString(gameNumber));
				wrapperRunner.callLong(mainRunner, 10000000, 500000000, "registry", gameNumber,
						addressKeccak256(result0), addressKeccak256(result1));				
			}

			ExampleUtilities.showResult("*** Getting the balance");
			long balance = wrapperRunner.callLong(mainRunner, 10000000, 0, "getBalance");
			ExampleUtilities.showResult("Balance of the Game  " + Long.toString(balance));


			/*
			 * Workflow
			 * 
			 * 3) ON-CHAIN: Try to solve the number (Sending reveal and Paying 1 Hbar); must
			 * send the reward figure
			 */
			ExampleUtilities.showResult("*** Revealing the secret number");

			for (int i = 0; i < 10; i++) {
				long ganador = (long) i;
				if (wrapperRunner.callBoolean(mainRunner, 10000000, 100000000, "revealOption",
						gameNumber, dappData, Long.toString(ganador), 500000000)) {
					ExampleUtilities.showResult("*** Winning number revealed " + Long.toString(ganador));
					if ( Long.toString(ganador) == witness) {
						ExampleUtilities.showResult("*** Witness EQUALS Winner number. Success!! ");
					} else {
						ExampleUtilities.showResult("*** Witness DOES NOT MATCH Winner number. Failed!!");
					}
					break;
				} else {
					ExampleUtilities.showResult("*** Lost 1 Hbar trying " + Long.toString(ganador));
				}
			}

			/*
			 * Workflow
			 * 
			 * 4) ONLINE: Get the balances
			 */

			ExampleUtilities.showResult("*** Getting the balance");
			balance = wrapperRunner.callLong(mainRunner, 10000000, 0, "getBalance");
			ExampleUtilities.showResult("Balance of the Game  " + Long.toString(balance));
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

	public static HederaContract readContract(String itemAddress) {
		HederaContract respuesta = new HederaContract();
		respuesta.shardNum = Integer.parseInt(itemAddress.substring(0, 3), 16);
		respuesta.realmNum = Integer.parseInt(itemAddress.substring(4, 11), 16);
		respuesta.contractNum = Integer.parseInt(itemAddress.substring(12), 16);
		return respuesta;
	}
}