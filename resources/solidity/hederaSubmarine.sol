//0x0000000000000000000000000000000000000000000000000000000000000000
//0x414c460000000000000000000000000000000000000000000000000000000000
//Online HEX: https://www.online-toolz.com/tools/text-hex-convertor.php

pragma solidity ^0.4.25;

import "./SafeMath.sol";

contract hashgraphSubmarine {

    using SafeMath for uint256;

    ////////////
    // Events //
    ////////////

    event Unlocked(
        bytes32 indexed _submarineId,
        uint96 _commitValue
    );
    event Revealed(
        bytes32 indexed _submarineId,
        uint96 _commitValue,
        string _witness,
        bytes32 _commitHash,
        address _submarineAddr
    );

    /////////////
    // Storage //
    /////////////

    // ** JSilver  **
    // Hedera does not process blocks but timestamp. We will resolve the reveal
    // by adding the block number to the timestamp and compare it to the block 
    // timestamp during the reveal. Put 20x1000 to map 20 secs, and move uint256
    
    uint256 public minRevealTimestamp = 0;

    // Stored "session" state information
    mapping(bytes32 => SubmarineSession) internal sessions;

    // A submarine send is considered "finished" when the amount revealed and
    // unlocked are both greater than zero, and the amount for the unlock is
    // greater than or equal to the reveal amount.
    struct SubmarineSession {
        // ** JSilver  **
        // Amount the reveal transaction revealed would be sent in tiniBar. 
        // This reflects the amount paid when submitting the submarine
        uint96 amount;
        // ** JSilver  **
        // Amount the unlock transaction in tinibar. When greater than
        // zero, the submarine has been unlocked; however the submarine may not
        // be finished, until the unlock amount is GREATER or EQUAL than the promised
        // revealed amount.
        uint96 amountUnlocked;
        // ** JSilver  **
        // Received timestamp when storing the Submarine (uint256)
        uint  commitFairstamp;
        // Second Key Hash with (sendingAddress+receivingAddress+Witness ) (uint256)
        bytes32 secondKey;
    }

    /////////////
    // Getters //
    /////////////

    /*
       Keeping these functions makes instantiating a contract more expensive for gas costs, but helps JAVA 
       development since we can reuse the org.ethereum.core.CallTransaction to manage the encoding of
       the transactions before sending to sha256
    */

    /**
     * @notice Helper function to return a submarine ID for associated given
     *         input data
     * @param _user address of the user that initiated the full submarine flow
     * @param _libsubmarine address of the contract. Usually this
     * @param _commitValue value commited to the pool
     * @param _commitOptionSelected Vote option
     * @param _witness Random number generated to provide proof of ownership
     * @param _embeddedDAppData additional data that should be revealed
     */

    function getSubmarineId(
        address _user,
        address _libsubmarine,
        uint256 _commitValue,
        bytes32 _commitOptionSelected,
        string memory _witness,
        bytes  _embeddedDAppData        
    ) public pure returns (bytes32 _key) {
        return keccak256(abi.encodePacked(
            _user,
            _libsubmarine,
            _commitValue,
            _commitOptionSelected,
            _witness,
            _embeddedDAppData
        ));
    }

    function getSecondKey(
        address _user,
        address _libsubmarine,
        string memory _witness
    ) public pure returns (bytes32 _secondKey) {
        return keccak256(abi.encodePacked(
            _user,
            _libsubmarine,
            _witness));
    }

    /**
     * @notice Return the session information associated with a submarine ID.
     * @return amountRevealed amount promised by user to be unlocked in reveal
     * @return amountUnlocked amount actually unlocked by the user at this time
     * @return commitTxBlockNumber block number that the user proved holds the
     *         commit TX.
     * @return commitTxIndex the index in the block where the commit tx is.
     */

    function getSubmarineState(bytes32 _submarineId) public view returns (
        uint96 amount,
        uint96 amountUnlocked,
        uint commitFairstamp,
        bytes32 secondKey
        
    ) {
        SubmarineSession memory sesh = sessions[_submarineId];
        return(
            sesh.amount,
            sesh.amountUnlocked,
            sesh.commitFairstamp,
            sesh.secondKey
        );
    }

   /**
     * @notice Singleton session getter - amount of money sent in submarine send
     * @return amountRevealed amount promised by user to be unlocked in reveal
     */
    function getSubmarineAmount(bytes32 _submarineId) public view returns (
        uint96 amount
    ) {
        SubmarineSession memory sesh = sessions[_submarineId];
        return sesh.amount;
    }

    /**
     * @notice Singleton session getter - Commit Timestamp recorded
     * @return commitFairstamp that was returned during transaction submit
     */
    function getSubmarineTimestamp(bytes32 _submarineId)
        public view returns (uint commitFairstamp)
    {
        SubmarineSession memory sesh = sessions[_submarineId];
        return sesh.commitFairstamp;
    }

    /////////////
    // Setters //
    /////////////

    // ** Original LibSubmarine **
    /**
     * @notice Consumers of this library should implement their custom reveal
     *         logic by overriding this method. This function is a handler that
     *         is called by reveal. A user calls reveal, LibSubmarine does the
     *         required submarine specific stuff, and then calls this handler
     *         for client specific implementation/handling.
     * @param  _submarineId the ID for this submarine workflow
     * @param _embeddedDAppData optional Data passed embedded within the unlock
     *        tx. Clients can put whatever data they want committed to for their
     *        specific use case
     * @param _value amount of ether revealed
     *
     */

    function onSubmarineReveal(
        bytes32 _submarineId,
        bytes _embeddedDAppData,
        uint256 _value
    ) internal;

    /**
     * @notice Function called by the user to reveal the owner (Receiving Address).
     * @param _embeddedDAppData optional Data passed embedded within the unlock
     * @param _witness Witness "secret" we committed to
     * @param _commitValue value submitted to the pool
     * @param _commitOptionSelected Option
     */
     
    function reveal(
        bytes32 submarineId,
        bytes _embeddedDAppData,
        string memory _witness,
        uint96 _commitValue,
        bytes32 _commitOptionSelected
    ) internal {
        require(
            block.timestamp.sub(minRevealTimestamp) > 0,
            "Wait for commitPeriodLength defined before revealing");

        require(
            sessions[submarineId].commitFairstamp != 0,
            "The tx is already revealed"
        );

        bytes32 secondKey = getSecondKey(
            msg.sender,
            address(this),
            _witness
        );

        require(
            sessions[submarineId].secondKey == secondKey,
            "Incorrect reveal Second Key"
        );

        // fullCommit = (addressA + addressC + aux(sendAmount) + dappData + w + aux(gasPrice) + aux(gasLimit))
        bytes32 submarineIdProof = getSubmarineId(
            msg.sender,
            address(this),
            _commitValue,
            _commitOptionSelected,
            _witness,
            _embeddedDAppData
        );
        if ( submarineIdProof != submarineId ) return;

        //Mark the line as revealed
        sessions[submarineId].commitFairstamp = 0;
        
        emit Revealed(
            submarineId,
            _commitValue,
            _witness,
            secondKey,
            msg.sender
        );

        onSubmarineReveal(
            submarineId,
            _embeddedDAppData,
            _commitValue
        );
    }

    /**
     * @notice Function called by the submarine address to unlock the session.
     * @dev warning this function does NO validation whatsoever.
     *      ALL validation is done in the reveal.
     * @param _submarineId committed data; The commit instance representing the
     *        commit/reveal transaction
     */
    function unlock( bytes32 _submarineId, address toWhom ) public payable {
        // Required to prevent an attack where someone would unlock after an
        // unlock had already happened, and try to overwrite the unlock amount.
        require(
            sessions[_submarineId].amountUnlocked <= sessions[_submarineId].amount,
            "You can never unlock less money than you've already unlocked."
        );
        require(
            sessions[_submarineId].commitFairstamp == 0,
            "You have to reveal the submarine first"
        );
        uint96 pay = sessions[_submarineId].amount- sessions[_submarineId].amountUnlocked;

        sessions[_submarineId].amountUnlocked = sessions[_submarineId].amount;
        if (pay > 0) {
            toWhom.transfer(pay);
        }
        emit Unlocked(_submarineId, uint96(pay));
    }

    /**
     * @notice revealedAndUnlocked can be called to determine if a submarine
     *         send transaction has been successfully completed for a given
     *         submarineId
     * @param _submarineId committed data; The commit instance representing the
     *        commit/reveal transaction
     * @return bool whether the commit has a stored submarine send that has been
     *         completed for it (0 for failure / not yet finished, 1 for
     *         successful submarine TX)
     */
    function revealedAndUnlocked(
        bytes32 _submarineId
    ) public view returns(bool success) {
        SubmarineSession memory sesh = sessions[_submarineId];
        return sesh.commitFairstamp == 0
            && sesh.amountUnlocked >= sesh.amount;
    }
}
