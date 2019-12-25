package exceptionTest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.CallParams;
import com.hedera.hashgraph.sdk.HederaException;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.contract.ContractCallQuery;
import com.hedera.hashgraph.sdk.contract.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.contract.ContractDeleteTransaction;
import com.hedera.hashgraph.sdk.examples.ExampleHelper;
import com.hedera.hashgraph.sdk.file.FileCreateTransaction;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;

import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.Instant;

public final class CreateSimpleContract {
	private CreateSimpleContract() {
	}

	public static void main(String[] args) throws HederaException, IOException, InterruptedException {
		var cl = CreateSimpleContract.class.getClassLoader();

		var gson = new Gson();

		JsonObject jsonObject;

		try (var jsonStream = cl.getResourceAsStream("hello_world.json")) {
			if (jsonStream == null) {
				throw new RuntimeException("failed to get hello_world.json");
			}

			jsonObject = gson.fromJson(new InputStreamReader(jsonStream), JsonObject.class);
		}

		var byteCodeHex = jsonObject.getAsJsonPrimitive("object").getAsString();

		var operatorKey = ExampleHelper.getOperatorKey();
		var client = ExampleHelper.createHederaClient();

		// Set Transaction Fees at the Client Level
		// Use reference https://docs.hedera.com/docs/testnet-pricing-table
		client.setMaxTransactionFee(42_000_000);
		// The memo field serves as future reconciliation of record entries in account
		String memoField = "Exception Trials";

		FileId newFileId;
		try {
			// create the contract's bytecode file
			var fileTx = new FileCreateTransaction(client)
					.setExpirationTime(Instant.now().plus(Duration.ofSeconds(3600))).addKey(operatorKey.getPublicKey())
					.setContents(byteCodeHex.getBytes())
					// Set the memo field and transaction Fee at the request
					.setMemo(memoField).setTransactionFee(1_700_000);

			var fileReceipt = fileTx.executeForReceipt();
			newFileId = fileReceipt.getFileId();

			System.out.println("contract bytecode file: " + newFileId);
		} catch (HederaException e0) {
			System.out.println(" **** Failed!! **** ");
			System.out.println("Error: " + e0.responseCode.toString());
			
		} finally {
			Thread.sleep(500);
			// This one has enough Fees to go through
			// create the contract's bytecode file
			var fileTx = new FileCreateTransaction(client)
					.setExpirationTime(Instant.now().plus(Duration.ofSeconds(3600))).addKey(operatorKey.getPublicKey())
					.setContents(byteCodeHex.getBytes())
					// Set the memo field and transaction Fee at the request
					.setMemo(memoField).setTransactionFee(42_700_000);

			var fileReceipt = fileTx.executeForReceipt();
			newFileId = fileReceipt.getFileId();
			System.out.println("\nThe second time contract goes through ");
			System.out.println("Contracts goes trough ");
			System.out.println("contract bytecode file: " + newFileId);
			
		}
		// create the contract itself
		// Gas is provided by EVM, and Fees are required to Pay for putting the
		// transaction through the network & file system
		var contractTx = new ContractCreateTransaction(client).setAutoRenewPeriod(Duration.ofHours(1)).setGas(217000)
				.setBytecodeFile(newFileId).setAdminKey(operatorKey.getPublicKey())
				// Set the memo field and transaction Fee at the request
				.setMemo(memoField).setTransactionFee(833_400_000);

		var contractReceipt = contractTx.executeForReceipt();

		System.out.println(contractReceipt.toProto());

		var newContractId = contractReceipt.getContractId();

		System.out.println("new contract ID: " + newContractId);

		boolean tryBalance = true;
		long cycle = 0;
		while (tryBalance) {
			try {
				System.out.println("\nTrying to raise a BUSY response");

				var contractCallResult = new ContractCallQuery(client).setGas(30000).setContractId(newContractId)
						.setFunctionParameters(CallParams.function("greet")).execute();
				// Getting entry default at QueryBuilder class

				if (contractCallResult.getErrorMessage() != null) {
					System.out.println("error calling contract: " + contractCallResult.getErrorMessage());
					return;
				}
				var message = contractCallResult.getString(0);
				System.out.println("contract message: " + message);
				long sizeFunction = CallParams.function("greet").addBool(true).toProto().size();
				System.out.println("bytes sent: " + sizeFunction);
				long gasUsed = contractCallResult.getGasUsed();
				System.out.println("gas used: " + gasUsed);
			} catch (HederaException e1) {
				if (e1.responseCode == ResponseCodeEnum.BUSY) {
					System.out.println("Found a BUSY response; keep trying");
					Thread.sleep(300);
				} else {
					break;
				}
			} catch (HederaNetworkException e2) {
				break;
			} finally {
				System.out.println("Number of iterations " + cycle);
				cycle += 1;
				if (cycle > 100)
					break;
			}
		}
		// Current Schedule requires 5_833_334 to delete a contract
		long transactionFee = 5_700_000;
		while (transactionFee < 6_500_000) {
			try {
				// now delete the contract
				var contractDeleteResult = new ContractDeleteTransaction(client).setContractId(newContractId)
						.setTransactionFee(transactionFee).executeForReceipt();

				if (contractDeleteResult.getStatus() != ResponseCodeEnum.SUCCESS) {
					System.out.println("error deleting contract: " + contractDeleteResult.getStatus());
					return;
				}
				System.out.println("Contract successfully deleted");
			} catch (HederaException e1) {
				if (e1.responseCode == ResponseCodeEnum.INSUFFICIENT_TX_FEE) {
					System.out.println("\nError: " + e1.responseCode.toString());
					transactionFee += 100_000;
					System.out.println("Increasing fees to: " + transactionFee);
				} else {
					break;
				}
			}
		}
	}
}
