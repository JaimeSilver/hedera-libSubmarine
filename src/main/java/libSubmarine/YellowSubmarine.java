package libSubmarine;

import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Random;

import org.bouncycastle.util.encoders.Hex;
import org.ethereum.crypto.HashUtil;
import org.ethereum.util.ByteUtil;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.CallParams;
import com.hedera.hashgraph.sdk.HederaException;
import com.hedera.hashgraph.sdk.contract.ContractCallQuery;
import com.hedera.hashgraph.sdk.contract.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.contract.ContractExecuteTransaction;
import com.hedera.hashgraph.sdk.contract.ContractId;
import com.hedera.hashgraph.sdk.examples.ExampleHelper;
import com.hedera.hashgraph.sdk.examples.advanced.CreateSimpleContract;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.file.FileCreateTransaction;
//import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;

import libSubmarine.fileWrappers.FileCreateFullTransaction;
import libSubmarine.utils.Numeric;
import java.util.Date;
import java.util.Random;

public final class YellowSubmarine {
	public static void main(String[] args) throws HederaException, IOException, InterruptedException {
		var cl = CreateSimpleContract.class.getClassLoader();

		// create a file and contract with contents
		boolean doFileContract = true;
		// validate Off-cain and On-chain SHA
		boolean doValidateSHA3 = false;
		// run and actual vote on the contract
		boolean runTheVote = true;

		String memoField = "Submarine Load";

		var operatorKey = ExampleHelper.getOperatorKey();
		var client = ExampleHelper.createHederaClient().setMaxTransactionFee(834_000_000);
		ContractId mainSubmarine = new ContractId(0, 0, 37200);

		if (doFileContract) {
			var gson = new Gson();

			JsonObject jsonObject;

			try (var jsonStream = cl.getResourceAsStream("thePoll.solidity")) {
				if (jsonStream == null) {
					throw new RuntimeException("Failed to load thePoll.solidity");
				}

				jsonObject = gson.fromJson(new InputStreamReader(jsonStream), JsonObject.class);
			}

			var byteCodeHex = jsonObject.getAsJsonPrimitive("object").getAsString();

			// create the contract's bytecode file
			FileId newFileId = new FileCreateFullTransaction(client)
					.setExpirationTime(Instant.now().plus(Duration.ofSeconds(3600)))
					// Use the same key as the operator to "own" this file
					.setContents(byteCodeHex.getBytes(), 420_000_000, memoField, operatorKey.getPublicKey());

			System.out.println("contract bytecode file: " + newFileId);

			// create the contract itself
			var contractTx = new ContractCreateTransaction(client).setAutoRenewPeriod(Duration.ofHours(1))
					.setGas(217000).setBytecodeFile(newFileId)
					// Put 40 seconds as DURATION
					.setConstructorParams(longKeccak256(40)).setAdminKey(operatorKey.getPublicKey()).setMemo(memoField);

			var contractReceipt = contractTx.executeForReceipt();

			// System.out.println( contractReceipt.toProto());
			var newContractId = contractReceipt.getContractId();
			System.out.println("The Submarine Contract is  " + newContractId);
			mainSubmarine = newContractId;
		} else {
			System.out.println("The Submarine Contract is  " + mainSubmarine.getContractNum());
		}

		/*
		 * Declare the variables for the Polling run
		 */
		String hedera0;
		String hedera1;
		String result0;
		String result1;
		Long voteNumber;

		/*
		 * This option shows the Off-chain and On-chain options of the LibSubmarine.
		 */
		byte[] dappData = new byte[1];
		byte[][] primaryKey = new byte[6][];
		byte[][] secondKey = new byte[3][];

		if (doValidateSHA3) {
			if (mainSubmarine.equals(new ContractId(0, 0, 0))) {
				return;
			}

			String address_from = "ca35b7d915458ef540ade6068dfe2f44e8fa733c";
			String address_to = "1526613135cbe54ee257c11dd17254328a774f4a";
			long sendAmount = 60000;

			/*
			 * The user can decide to use Randomizer or External String
			 */

			Random rand = new Random();
			long numb = Math.abs(rand.nextLong());
			String witness = Long.toString(numb);

			/*
			 * Getting the first Key (Local and with ETH
			 */

			secondKey[0] = addressKeccak256(address_from);
			secondKey[1] = addressKeccak256(address_to);
			witness = "LA VACA LOLA";
			secondKey[2] = stringKeccak256(witness);
			result1 = Hex.toHexString(HashUtil.sha3(ByteUtil.merge(secondKey)));

			// The call to the Hashgraph for the Secondary Key
			var contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("getSecondKey").addAddress(address_from)
							.addAddress(address_to).addString(witness))
					.execute();
			if (contractCallResult.getErrorMessage() != null) {
				System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
				return;
			}

			hedera1 = Hex.toHexString(contractCallResult.getRawValue(0).toByteArray());

			/*
			 * Getting the first Key (Local and with ETH
			 */

			primaryKey[0] = addressKeccak256(address_from);
			primaryKey[1] = addressKeccak256(address_to);
			primaryKey[2] = longKeccak256(sendAmount);
			String optionSelected = "ALF";
			primaryKey[3] = char32Keccak256(optionSelected);
			primaryKey[4] = stringKeccak256(witness);
			primaryKey[5] = dappData;
			result0 = Hex.toHexString(HashUtil.sha3(ByteUtil.merge(primaryKey)));

			// The call to the Hashgraph for the Primary Key
			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("getSubmarineId").addAddress(address_from)
							.addAddress(address_to).addUint(sendAmount, 256).addBytes(castBytes32(optionSelected), 32)
							.addString(witness).addBytes(dappData))
					.execute();
			if (contractCallResult.getErrorMessage() != null) {
				System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
				return;
			}

			hedera0 = Hex.toHexString(contractCallResult.getRawValue(0).toByteArray());

			// Show results on log System.out.println("Local Witness " + witness);
			System.out.println("Local Secondary Key  " + result1);
			System.out.println("Hedera Second Key    " + hedera1);

			System.out.println("Local Submarine Key  " + result0);
			System.out.println("Hedera Submarine Key " + hedera0);
		}

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
			 * Get Primary Key: ----> Input: Witnessing Address (address) ----> Contract
			 * Address (address) ----> Witness string to prove origin (string) <---- Output:
			 * SecondaryKey in HEX
			 * 
			 * Get Secondary Key: ----> Input: Witnessing Address (address) ----> Contract
			 * Address (address) ----> Commit Value (uint256) In case user wants to support
			 * payments ----> Option Selected (char32) Option selected for the polling
			 * options ----> Witness string (Secret Phrase) (string) ----> Embedded Data.
			 * Extension as suggested by libSubmarines <---- Output: PrimaryKey in HEX
			 */

			// userAddress is using the Paying Address;
			// user can decide the Address sender that will call the Reveal

			String submarineAddress = mainSubmarine.toSolidityAddress();
			var operatorId = ExampleHelper.getOperatorId();
			String userAddress = operatorId.toSolidityAddress();
			primaryKey[0] = addressKeccak256(userAddress);
			primaryKey[1] = addressKeccak256(submarineAddress);
			primaryKey[2] = longKeccak256(0);
			String optionSelected = "ALF";
			primaryKey[3] = char32Keccak256(optionSelected);

			// Option for randomizer or external phrase
			Random rand = new Random();
			long numb = Math.abs(rand.nextLong());
			String witness = Long.toString(numb);
			witness = "LA VACA LOLA";

			primaryKey[4] = stringKeccak256(witness); // Secret Phrase
			primaryKey[5] = dappData;
			result0 = Hex.toHexString(HashUtil.sha3(ByteUtil.merge(primaryKey)));

			/*
			 * Clearly you will not send this to the Network BEFORE reveal it is provided
			 * for testing purposes.
			 */

			// The call to the Hashgraph for the Primary Key
			var contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("getSubmarineId").addAddress(userAddress)
							.addAddress(submarineAddress).addUint(0, 256).addBytes(castBytes32(optionSelected), 32)
							.addString(witness).addBytes(dappData))
					.execute();

			hedera0 = Hex.toHexString(contractCallResult.getRawValue(0).toByteArray());

			System.out.println("Local Submarine Key  " + result0);
			System.out.println("Hedera Submarine Key " + hedera0);

			secondKey[0] = addressKeccak256(userAddress);
			secondKey[1] = addressKeccak256(submarineAddress);
			secondKey[2] = stringKeccak256(witness);

			result1 = Hex.toHexString(HashUtil.sha3(ByteUtil.merge(secondKey)));

			// The call to the Hashgraph for the Secondary Key
			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("getSecondKey").addAddress(userAddress)
							.addAddress(submarineAddress).addString(witness))
					.execute();
			if (contractCallResult.getErrorMessage() != null) {
				System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
				return;
			}

			hedera1 = Hex.toHexString(contractCallResult.getRawValue(0).toByteArray());

			System.out.println("Local Secondary Key  " + result1);
			System.out.println("Hedera Second Key    " + hedera1);

			/*
			 * Workflow
			 * 
			 * 2a) ON-CHAIN: The system can control voting times and reveal times. In this
			 * case they are the same
			 */

			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("minRevealTimestamp")).execute();
			if (contractCallResult.getErrorMessage() != null) {
				System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
				return;
			}

			Long voteReveal = contractCallResult.getLong(0);
			Date date = new Date(voteReveal * 1000L);
			System.out.println("Minimum Reveal Timestamp " + date.toString());

			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("minReadTimestamp")).execute();
			if (contractCallResult.getErrorMessage() != null) {
				System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
				return;
			}

			Long voteRead = contractCallResult.getLong(0);
			date = new Date(voteRead * 1000L);
			System.out.println("Minimum Read Timestamp   " + date.toString());

			/*
			 * Make sure that the options are in the Contract
			 */
			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("isOption").addBytes(castBytes32("KERMIT"), 32))
					.execute();
			if (!contractCallResult.getBool(0)) {
				System.out.println("*** Creating KERMIT in Contract");
				new ContractExecuteTransaction(client).setGas(10_000_000).setContractId(mainSubmarine)
						.setFunctionParameters(CallParams.function("addOptions").addBytes(castBytes32("KERMIT"), 32))
						.execute();
			}

			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("isOption").addBytes(castBytes32("ALF"), 32)).execute();
			if (!contractCallResult.getBool(0)) {
				System.out.println("*** Creating ALF in Contract");
				new ContractExecuteTransaction(client).setGas(10_000_000).setContractId(mainSubmarine)
						.setFunctionParameters(CallParams.function("addOptions").addBytes(castBytes32("ALF"), 32))
						.execute();
			}

			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("isOption").addBytes(castBytes32("DORA"), 32)).execute();
			if (!contractCallResult.getBool(0)) {
				System.out.println("*** Creating DORA in Contract");
				new ContractExecuteTransaction(client).setGas(10_000_000).setContractId(mainSubmarine)
						.setFunctionParameters(CallParams.function("addOptions").addBytes(castBytes32("DORA"), 32))
						.execute();
			}

			Thread.sleep(5000);
			/*
			 * Workflow
			 * 
			 * 2b) ON-CHAIN: Register the Primary and secondary keys
			 */
			System.out.println("*** Casting the vote");

			// Note that the option is not sent; the Submarine tries the options to unveil
			// the vote.
			var contractExecuteResult = new ContractExecuteTransaction(client).setGas(10_000_000)
					.setContractId(mainSubmarine)
					.setFunctionParameters(
							CallParams.function("registry")
							.addBytes(Numeric.hexStringToByteArray(result0), 32)
							.addBytes(Numeric.hexStringToByteArray(result1), 32))
					.executeForRecord();
			Thread.sleep(500);
			var newIndex = contractExecuteResult.getCallResult().getLong(0);
			System.out.println("New vote registered at index " + Long.toString(newIndex));

			System.out.println("*** countPoll   may revert if it is not time yet");

			try {
				contractExecuteResult = new ContractExecuteTransaction(client).setGas(10_000_000)
						.setContractId(mainSubmarine).setFunctionParameters(CallParams.function("countPoll"))
						.executeForRecord();
				Thread.sleep(500);
				voteNumber = contractExecuteResult.getCallResult().getLong(0);
				System.out.println("Number of Votes          " + Long.toString(voteNumber));
			} catch (Exception e) {
				System.out.println("*** YEP failed!! ");
			}

			System.out.println("*** Waiting 40 seconds");
			Thread.sleep(40000);

			/*
			 * Workflow
			 * 
			 * 3) ON-CHAIN: Address registered in the submarine sends the Reveal
			 */

			System.out.println("*** Revealing the vote");
			contractExecuteResult = new ContractExecuteTransaction(client).setGas(10_000_000)
					.setContractId(mainSubmarine)
					.setFunctionParameters(
							CallParams.function("revealOption")
							.addBytes(Numeric.hexStringToByteArray(result0), 32)
							.addBytes(dappData)
							.addString(witness)
							.addUint(0, 96))
					.executeForRecord();

			if (contractExecuteResult.getCallResult().getBool(0)) {
				System.out.println("*** Voting was revealed                          ");
				System.out.println("*** Waiting for Consensus to replicate the State ");
				Thread.sleep(10000);
			}

			/*
			 * Workflow
			 * 
			 * 4) ONLINE: Get the results NOTE: Hedera makes block.timestamp = fair order of
			 * the TX consensus; you need to submit CallFunctions, not CallLocal, to force
			 * an update of the status of the State variables (e.g. block.timestamp).
			 */

			System.out.println("*** Getting the vote");
			contractExecuteResult = new ContractExecuteTransaction(client).setGas(10_000_000)
					.setContractId(mainSubmarine).setFunctionParameters(CallParams.function("countPoll"))
					.executeForRecord();
			voteNumber = contractExecuteResult.getCallResult().getLong(0);

			System.out.println("Number of Votes       " + Long.toString(voteNumber));

			contractExecuteResult = new ContractExecuteTransaction(client).setGas(10_000_000)
					.setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("getTally").addBytes(castBytes32("ALF"), 32))
					.executeForRecord();
			Long votesALF = contractExecuteResult.getCallResult().getLong(0);;
			System.out.println("Votes for ALF " + Long.toString(votesALF));

			contractExecuteResult = new ContractExecuteTransaction(client).setGas(10_000_000)
					.setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("getTally").addBytes(castBytes32("KERMIT"), 32))
					.executeForRecord();
			Long votesKERMIT = contractExecuteResult.getCallResult().getLong(0);;
			System.out.println("Votes for KERMIT " + Long.toString(votesKERMIT));

			contractExecuteResult = new ContractExecuteTransaction(client).setGas(10_000_000)
					.setContractId(mainSubmarine)
					.setFunctionParameters(CallParams.function("getTally").addBytes(castBytes32("DORA"), 32))
					.executeForRecord();
			Long votesDORA = contractExecuteResult.getCallResult().getLong(0);;
			System.out.println("Votes for DORA " + Long.toString(votesDORA));

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

	public static byte[] castBytes32(String param) {
		byte[] ret = new byte[32];
		byte[] bytes = ((String) param).getBytes(StandardCharsets.UTF_8);
		if (bytes.length <= 32) {
			System.arraycopy(bytes, 0, ret, 0, bytes.length);
		} else {
			System.arraycopy(bytes, 0, ret, 0, 32);
		}
		return ret;
	}

}
