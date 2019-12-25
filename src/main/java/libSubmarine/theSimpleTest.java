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
import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.CallParams;
import com.hedera.hashgraph.sdk.HederaException;

import com.hedera.hashgraph.sdk.contract.ContractCallQuery;
import com.hedera.hashgraph.sdk.contract.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.contract.ContractId;
import com.hedera.hashgraph.sdk.examples.ExampleHelper;
import com.hedera.hashgraph.sdk.examples.advanced.CreateSimpleContract;
import com.hedera.hashgraph.sdk.file.FileId;

import libSubmarine.fileWrappers.FileCreateFullTransaction;
import libSubmarine.utils.Numeric;

public final class theSimpleTest {
	public static void main(String[] args) throws HederaException, IOException, InterruptedException {
		var cl = CreateSimpleContract.class.getClassLoader();

		// create a file and contract with contents
		boolean doFileContract = false;
		// run and actual vote on the contract
		boolean runTest = true;

		String memoField = "Simple Test Load";

		var operatorKey = ExampleHelper.getOperatorKey();
		var client = ExampleHelper.createHederaClient().setMaxTransactionFee(834_000_000);
		ContractId testContract = new ContractId(0, 0, 37113);

		if (doFileContract) {
			var gson = new Gson();

			JsonObject jsonObject;

			try (var jsonStream = cl.getResourceAsStream("theSimpleTest2.solidity")) {
				if (jsonStream == null) {
					throw new RuntimeException("Failed to load theSimpleTest2.solidity");
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
					.setMemo(memoField);

			var contractReceipt = contractTx.executeForReceipt();

			var newContractId = contractReceipt.getContractId();
			System.out.println("The Submarine Contract is  " + newContractId);
			testContract = newContractId;
		} else {
			System.out.println("The Simple Test is  " + testContract.getContractNum());
		}

		if (runTest) {

			String address_from = "ca35b7d915458ef540ade6068dfe2f44e8fa733c";
			long sendAmount = 60000;
			String text = "This is a test";
			String option = "ALF";

			// The call to the Hashgraph to test Type Long
			var contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(testContract)
					.setFunctionParameters(CallParams.function("add").addUint(1, 256).addUint(2, 256)).execute();
			if (contractCallResult.getErrorMessage() != null) {
				System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
				return;
			}
			long Numeral = contractCallResult.getLong(0);			
			System.out.println("Ping Long is   " + Long.toString(Numeral));
			
			// The call to the Hashgraph to test Type String
			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(testContract)
					.setFunctionParameters(CallParams.function("pingString").addString(text)).execute();
			if (contractCallResult.getErrorMessage() != null) {
				System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
				return;
			}
			String pingString = contractCallResult.getString(0);
			System.out.println("Ping String is " + pingString);

			// The call to the Hashgraph to test Type Address
			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(testContract)
					.setFunctionParameters(CallParams.function("pingAddress")
							.addAddress(address_from)).execute();
			if (contractCallResult.getErrorMessage() != null) {
				System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
				return;
			}
			String pingAddress = Hex.toHexString(contractCallResult.getAddress(0).toByteArray());
			System.out.println("Ping String is  " + pingAddress);

			// The call to the Hashgraph to test Type Bytes
			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(testContract)
					.setFunctionParameters(CallParams.function("pingBytes")
							.addBytes(address_from.getBytes())).execute();
			if (contractCallResult.getErrorMessage() != null) {
				System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
				return;
			}
			
			String pingBytes = new String(contractCallResult.getBytes(0).toByteArray());
			System.out.println("Ping Bytes is   " + pingBytes);
			
			// The call to the Hashgraph to test Type Char32
			//contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(testContract)
			//		.setFunctionParameters(CallParams.function("pingBytes32")
			//				.addBytes32(option)).execute();
			contractCallResult = new ContractCallQuery(client).setGas(10_000_000).setContractId(testContract)
					.setFunctionParameters(CallParams.function("pingBytes32")
							.addBytes(castBytes32(option),32)).execute();			
			if (contractCallResult.getErrorMessage() != null) {
				System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
				return;
			}
			
			String pingBytes32 = contractCallResult.getRawValue(0).toStringUtf8();
			System.out.println("Ping Bytes32 is " + pingBytes32);
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
    	System.arraycopy(bytes, 0, ret, 0, bytes.length);
        return ret;
    }
}
