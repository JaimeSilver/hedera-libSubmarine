package exceptionTest;
import com.hedera.hashgraph.sdk.HederaException;
import com.hedera.hashgraph.sdk.HederaNetworkException;
import com.hedera.hashgraph.sdk.examples.ExampleHelper;
import com.hedera.hashgraph.sdk.proto.ResponseCodeEnum;

public final class GetAccountBalance {
	private GetAccountBalance() {
	}

	public static void main(String[] args) throws HederaException {
		var operatorId = ExampleHelper.getOperatorId();
		var client = ExampleHelper.createHederaClient();
		boolean tryBalance = true;
		long cycle = 0;
		while (tryBalance) {
			try {
				var balance = client.getAccountBalance(operatorId);
				System.out.println("balance = " + balance);
			} catch (HederaException e1) {
				if (e1.responseCode == ResponseCodeEnum.BUSY) {
					System.out.println("Found a BUSY response; keep trying");
					try {
						Thread.sleep(300);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				} else {
					break;
				}
			} catch (HederaNetworkException e2) {
				break;
			} finally {
				System.out.println("Number of iterations " + cycle);				
				cycle += 1;
				if (cycle > 100) break; 
			}
		}
	}
}
