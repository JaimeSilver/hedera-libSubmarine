# hedera-libSubmarine
Implementation of Submarines in Hedera Hashgraph

This repository follows some of the design patters proposed by libSubmarines
(https://libsubmarine.org/) for Ethereum, but goes beyond defeating Front-Runners, 
since that is already solved by Hashgraph's Gossip-about-Gossip and Fair timestamping.

The model proposed facilitates transaction calls to contracts with the later reveal of 
the contents of a message. I have included a library in Solidity that servers as a 
reference for the actual contract, including remuneration during the UnLock step.


**This is a work-in-progress implementation of better submarine sends for Hashgraph.**

# Submarine Steps @Hedera

- `1` Alice generates a Primary and Secondary key Off-Chain 
- `2` Alice sends transaction to an existing submarine (SC)
- `3` The address earmarked in the keys of A (Alice or other Hedera Account or Smart Contract) 
      reveals the keys, and emits an event.
- `4` Once the keys are revealed, the data could be stored for further use. I have included
      a model in which the answer is revealed without the Message Sender knowing the answer
      to emulate Zero Knowledge. This requires that the options provided are discrete or 
      at least bounded. The final contract can then return summarized answers ONLY, without showing 
      the Alice's selection.
- `5` Transaction is unlocked, and could be trigger payable events (e.g. rewards or other 
      payments).

-----------
# Workflow:
- Alice generates keys offline
- Alice builds transaction and sends to Hedera EVM (Online).
  NOTE: Memo field could be usedto transport the secret phrase or passed to the intended receipient 
  off-line. No one but the intended receipient can reveal the transaction, and cannot occur
  before predefined time.
- (Time passes)
- Address defined by Alice needs to send a transaction that reveals the content of the initial sent.
- The Submarine keeps the contents private, releasing only summarized data. I left the broadcasting events to
  facilitate comparison to the original libSubmarine.

-----------
# Test case 

Prerequisites:
Install Java SDK for Hedera; validate release (currently model was developed with 0.3.0).
Run MVN Install for the project; the POM provided has all requisites, and also generates a runnable 
JAR in the Root folder with a -run (suffix).
Complete the the node.properties with your Private and Public keys.


The test provided is a Poll with 3 alternatives (ALF, DORA, KERMIT); Alice decides her vote, and sends 
the transaction.
I provided smart contract functions to compare the results Online and Offline, following the guidelines 
of libSubmarines.

The vote is added to the Smart Contract; but since Reads are time-protected in the contract, the function
calls can only complete when certain time has passed (defined for the Constructor as DURATION).

Once DURATION, Alice sends the reveal (without specifying her vote); the SC certifies that 
the Secret Phrase can generate the Second Key, and with that the Submarine validates the full message given 
can generate the primary key.

In this example, the Option selected by Alice is not provided, so the Smart Contract iterates for an 
array of alternatives to validate a match; the correct combination is stored in the Results table (private).
Then, the Smart Contract triggers the Unlock Function and closes the submarine.

Reads are made using full calls to update the Block-timestamp provided by the Consensus.

-------------
# Other use cases
The model is useful for multiple applications, including Voting, Auctions, Close Bids, and Gambling Dapps.
Please review the GitHub repository of libSubmarine (https://github.com/lorenzb/libsubmarine), since
some of the Solidity contracts presented (ERC20 cover pruchases & ERC721 Auction ) could be extended
for Hedera.

-------------
# Disclaimer
This project is a Work in Progress. It has not undergone a formal security audit from an independent 
3rd party (though we would like to have that done).

-----------
## Authors

This work was done by Jaime Plata (@JSilver - jp@aochain.net). It is released under the MIT license; all contributions 
are well received.

Hedera Hashgraph - Developer Mainnet Program - 2019
