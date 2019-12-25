package libSubmarine.fileWrappers;

import com.google.protobuf.ByteString;
import com.hedera.hashgraph.sdk.Client;
import com.hedera.hashgraph.sdk.HederaException;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.TimestampHelper;
import com.hedera.hashgraph.sdk.TransactionBuilder;
import com.hedera.hashgraph.sdk.contract.ContractCreateTransaction;
import com.hedera.hashgraph.sdk.crypto.Key;
import com.hedera.hashgraph.sdk.file.FileCreateTransaction;
import com.hedera.hashgraph.sdk.file.FileId;
import com.hedera.hashgraph.sdk.file.FileAppendTransaction;
import com.hedera.hashgraph.sdk.proto.*;
import io.grpc.MethodDescriptor;

import javax.annotation.Nullable;

import java.time.Duration;
import java.time.Instant;

public final class FileCreateFullTransaction extends TransactionBuilder<FileCreateFullTransaction> {
	private final FileCreateTransactionBody.Builder builder = bodyBuilder.getFileCreateBuilder();

	public FileCreateFullTransaction(@Nullable Client client) {
		super(client);
	}

	public FileCreateFullTransaction addOperatorKey(Key key) {
		return this;
	}

	public FileCreateFullTransaction setExpirationTime(Instant expiration) {
		builder.setExpirationTime(TimestampHelper.timestampFrom(expiration));
		return this;
	}

	public FileId setContents(byte[] contents, long _fee, String _memo, Key operatorKey) {
		System.out.println("FILE CREATE    ");

		final int FILE_PART_SIZE = 3300; // 3K bytes

		int numParts = contents.length / FILE_PART_SIZE;
		int remainder = contents.length % FILE_PART_SIZE;

		byte[] firstPartBytes = null;
		if (contents.length <= FILE_PART_SIZE) {
			firstPartBytes = contents;
			remainder = 0;
		} else {
			firstPartBytes = copyBytes(0, FILE_PART_SIZE, contents);
		}

		System.out.println("@@@ file size=" + contents.length);
		System.out.println("     FILE_PART_SIZE=" + FILE_PART_SIZE);
		System.out.println("     numParts=      " + numParts);
		System.out.println("     remainder=" + remainder);

		// create the contract's bytecode file
		var fileTx = new FileCreateTransaction(client).setExpirationTime(Instant.now().plus(Duration.ofSeconds(3600)))
				// Use the same key as the operator to "own" this file
				.addKey(operatorKey).setContents(firstPartBytes).setTransactionFee(_fee).setMemo(_memo);

		FileId newFileId;
		try {
			var fileReceipt = fileTx.executeForReceipt();
			newFileId = fileReceipt.getFileId();
			if (fileReceipt.getStatus() == ResponseCodeEnum.SUCCESS) {
				System.out.println("The new file number is " + newFileId);

				// append the rest of the parts
				for (int i = 1; i < numParts; i++) {
					byte[] partBytes = copyBytes(i * FILE_PART_SIZE, FILE_PART_SIZE, contents);
					// create the contract's bytecode file
					var fileAppend = new FileAppendTransaction(client).setFileId(newFileId).setContents(partBytes)
							.setTransactionFee(_fee).setMemo(_memo);

					try {
						var appendReceipt = fileAppend.executeForReceipt();
					} catch (HederaNetworkException e) {
						System.out.println("ERROR Appending file: HederaNetworkException");
						e.printStackTrace();
						return null;
					} catch (HederaException e) {
						System.out.println("ERROR Appending file: HederaException");
						e.printStackTrace();
						return null;
					}
				}
			} else {
				System.out.println("ERROR Appending file " + fileReceipt.getStatus().toString());
			}
		} catch (HederaNetworkException e) {
			System.out.println("ERROR Creating file: HederaNetworkException");
			e.printStackTrace();
			return null;
		} catch (HederaException e) {
			System.out.println("ERROR Creating file: HederaException");
			e.printStackTrace();
			return null;
		}

		if (remainder > 0) {
			byte[] partBytes = copyBytes(numParts * FILE_PART_SIZE, remainder, contents);
			var fileAppend = new FileAppendTransaction(client).setFileId(newFileId).setContents(partBytes)
					.setTransactionFee(_fee).setMemo(_memo);

			try {
				var appendReceipt = fileAppend.executeForReceipt();
			} catch (HederaNetworkException e) {
				System.out.println("ERROR Appending file: HederaNetworkException");
				e.printStackTrace();
				return null;
			} catch (HederaException e) {
				System.out.println("ERROR Appending file: HederaException");
				e.printStackTrace();
				return null;
			}
		}

		return newFileId;
	}

	public static byte[] copyBytes(int start, int length, byte[] bytes) {
		byte[] rv = new byte[length];
		for (int i = 0; i < length; i++) {
			rv[i] = bytes[start + i];
		}
		return rv;
	}

	@Override
	protected void doValidate() {
		require(builder.getKeysOrBuilder().getKeysOrBuilderList(), ".addKey() required");
	}

	@Override
	protected MethodDescriptor<Transaction, TransactionResponse> getMethod() {
		return FileServiceGrpc.getCreateFileMethod();
	}
}
