# hedera-libSubmarine
Implementation of Submarines in Hedera Hashgraph.

Submarines are designed to hide information from the network temporarily, and permit later reveals in Smart Contracts that are prepared to accept them. This mechanism is useful for many scenarios where anonymity or secrecy is vital, like silent bidding, voting, and closed auctions.

This repository follows some of the design patterns proposed by libSubmarines (https://libsubmarine.org/) for Ethereum, going beyond defeating Front-Runners which are already solved by Hedera Hashgraph's Gossip-about-Gossip and Fair timestamping Consensus Algorithm features. This pattern does not make use of Submarine Addresses since they are not supported by Hedera; instead, the Smart Contract payable address is used to transfer and receive Hbars.

This model facilitates transaction calls to contracts where the message’s content is revealed (fully or partially) at a later time. I have included a library in Solidity that serves as a reference for the Poll contract; the model can be extended to include remuneration during the UnLock stage (See Workflow).

**This is a work-in-progress implementation of better submarine sends for Hashgraph.**

# Submarine Steps @Hedera

- `1` Alice generates a Primary and Secondary key Off-Chain 
- `2` Alice sends the transaction to an existing submarine (SC)
- `3` The address earmarked in the keys of A (Alice or other Hedera Account or Smart Contract) 
      reveals the keys and emits an event.
- `4` Once the keys are revealed, the data could be stored for further use. I have included
      a model in which the answer is revealed without the Message Sender knowing the answer
      to emulate Zero Knowledge. This requires that the options provided are discrete or 
      at least bounded. The final contract can then return aggregated data only, without showing 
      Alice's selection (Note: as the Java example has only 1 entry, it is not difficult to guess the detail; i.e., there is not much to hide with a single entry!)  
- `5` The transaction is unlocked, and could trigger payable events (e.g. rewards or other 
      payments).

-----------
# Workflow:
- Alice generates keys offline, including her option (for full or partial message reveal), payment and secret phrase. Note that the Secret Phrase can be provided (using a randomizer or provided by the user). Both options are included in the code.
- Alice sends the transaction with the keys and sends to Hedera EVM (On-chain).
  NOTE: Memo field may be used to broadcast the secret phrase or passed only to the intended recipient 
  off-chain. No one but the intended recipient can reveal the transaction. The reveal cannot occur
  before the predefined time.
- (Time passes)
- The address defined by Alice sends a transaction to reveal the content of the initial send. Data is stored in a Results structure, to be used for querying the results (if desired).
- The Submarine keeps the contents private, releasing only summarized data. I left the broadcasting events and local calls to facilitate comparison to the original libSubmarine.

-----------
## Test case Poll 

Prerequisites:
Install Java SDK for Hedera; validate release (currently model was developed with 0.3.0).
Then run a MVN Install for the project; the POM provided has all requisites, and also generates a runnable JAR in the Root folder with a -run (suffix).
Complete the node.properties with your Private and Public keys.


The test provided is a Poll with 3 alternatives (ALF, DORA, KERMIT); Alice decides her vote and sends 
the transaction.
I provided smart contract functions to compare the results Onchain and Offchain function modules.

The vote is added to the Smart Contract (SC) ; because Reads are time-protected in the contract, the function
calls can only return results when a certain amount of time has passed (defined for the Constructor as DURATION).

Once DURATION, Alice sends the reveal (without specifying her vote). The SC certifies that 
the Secret Phrase can generate the Second Key and the Submarine validates the full message passed can unequivocally generate the primary key. Then the submarine session is closed.

In this example, the Option selected by Alice is not provided so the Smart Contract iterates for an 
array of alternatives to validate a match. The correct combination is stored in the private Results table then the Smart Contract triggers the Unlock Function and closes the submarine.

Reads are made using full calls to update the Block-timestamp provided by the Hedera Consensus.

-----------
## Test case Guessing Game

This pattern uses 2 contracts: one to manage the submarine sessions and the other to bank/route the incoming Hbars and guesses.

The Dapp Off-chain calculates a random number (00 to 99) and assigns it as the Witness Phrase. Since the two addresses are known, the Dapp can derive the Second Key.

The primary key is generated adding the Prize amount to be paid if the winner solves the puzzle: 500 tinibars.
All other parameters are left blank (null).

The Bank Smart Contract receives the total Prize amount from Alice in a Payable Call and serves as custodian.
Bob submits the first guess to the bank paying 10 tinibars which the Bank-SC mirrors it to the Submarine. If that number solves the puzzle, the contracts rewards the original msg sender (tx.origin) with the msg. The submarine session is then complete. 

-------------
## Other Use Cases
This model is useful for multiple applications, including Voting, Auctions, Close Bids, and Gambling Dapps.
Please review the GitHub repository of libSubmarine (https://github.com/lorenzb/libsubmarine), to discover how the Solidity contracts presented (ERC20 cover purchases & ERC721 Auction ) could be extended for Hedera Hashgraph.

-------------
## Disclaimer
This project is a Work in Progress. It has not undergone a formal security audit from an independent 
3rd party.

-----------
## Authors

This work was done by Jaime Plata (@JSilver - jp@aochain.net). It is released under the MIT license; all contributions 
are well received.

Hedera Hashgraph - Developer Mainnet Program - 2019
