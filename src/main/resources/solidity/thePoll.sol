//0x0000000000000000000000000000000000000000000000000000000000000000
//0x414c460000000000000000000000000000000000000000000000000000000000
//Dice ALF https://www.online-toolz.com/tools/text-hex-convertor.php

pragma solidity ^0.4.25;

import "./hashgraphSubmarine.sol";
import "./SafeMath.sol";

/**
 * Example Hidden contract for storing a secret to be revealed in the future
 *
 * Note: *********** PLEASE DONT USE THIS CODE IN PRODUCTION ***************
 * this contract is just an example of how hasgraphSubmarine can be used.
 * The code has not been designed to be efficient, performant, or secure,
 * only easy to understand.
 * This code has multiple problems.
 * it is JUST AN EXAMPLE.
 */
contract thePoll is hashgraphSubmarine {
    using SafeMath for uint256;

    //
    // STORAGE
    //

    uint256 public minReadTimestamp;
    
    address owner;
    
    struct DataStruct {
        bytes32 optionSelected;
        uint index;
    }
  
    mapping(bytes32 => DataStruct) internal pollResults;
    mapping(bytes32 => uint ) private pollOptions;
    bytes32[] public optionsToSelect;
    bytes32[] internal pollIndex;
    event LogNewPoll   (address indexed userAddress, uint index, bytes32 pollSession);

    /// MODIFIERS
    modifier onlyOwner() {
        require(msg.sender == owner);
        _;
    }

    /**
     * @notice Constructor, creates the Contract.
     */
    constructor( uint duration ) public {
        owner = msg.sender;
        minRevealTimestamp = block.timestamp + duration; //Set the duration to 20 secs
        minReadTimestamp = minRevealTimestamp;
    }

    /**
     * @notice Validate if session exists
     * @param option the Option to validate
     */
    function isOption(bytes32 option)
        public view returns(bool isIndeed) 
    {
        if(optionsToSelect.length == 0) return false;
        return (optionsToSelect[pollOptions[option]] == option);
    }

    /**
     * @notice Add options to select
     * @param optionToAdd the Option to add
     */
    function addOptions(bytes32 optionToAdd)
        public returns(uint index) 
    {
        if(isOption(optionToAdd)) revert();
        pollOptions[optionToAdd]  = optionsToSelect.push(optionToAdd)-1;
        return optionsToSelect.length-1;
    }

    /**
     * @notice Validate if session exists
     * @param _submarineId the ID associated 
     */
    function isSession(bytes32 _submarineId)
        public view returns(bool isIndeed) 
    {
        if(pollIndex.length == 0) return false;
        return (pollIndex[pollResults[_submarineId].index] == _submarineId);
    }

    /**
     * @notice Validate if session exists
     * @param submarineId the ID associated 
     */
    function insertPoll(
        bytes32 submarineId ) 
        private
        returns(uint index)
    {
        if(isSession(submarineId)) revert();
        //Nothing is recorded; have to wait until Reveal Occurs
        //pollResults[submarineId].optionSelected: Nothing recorded yet
        pollResults[submarineId].index  = pollIndex.push(submarineId)-1;
        emit LogNewPoll( msg.sender, pollResults[submarineId].index,  submarineId);
        return pollIndex.length-1;
    }
    
    /**
     * Return results
     */
    function countPoll( ) view public
        returns(uint256 results)
    {
        require(block.timestamp >= minReadTimestamp, "Cannot read the results YET");
        if (pollIndex.length == 0) return 0;
        return pollIndex.length;
    }
    
    /**
     * Return results
     */
    function getTally( bytes32 _optionSelected ) view public
        returns( uint256 total )
    {
        require(block.timestamp >= minReadTimestamp, "Cannot read the results YET");
        if (pollIndex.length == 0) return 0;
        total = 0;
        for (uint i = 0; i < pollIndex.length; i++){
            if ( pollResults[pollIndex[i]].optionSelected == _optionSelected )
             total = total + 1;
        }
        return total;
    }

    /**
     * @notice Sender enters poll
     * @param _submarineId the ID associated 
     * @param _secondId the Second ID associated
     */
    function registry(bytes32 _submarineId, bytes32 _secondId ) public returns (uint index)
    {
        index = insertPoll(_submarineId);
        sessions[_submarineId].amount = uint96(msg.value);
        sessions[_submarineId].amountUnlocked = uint96(msg.value);
        sessions[_submarineId].commitFairstamp = block.timestamp;
        sessions[_submarineId].secondKey = _secondId;
        return index;
    }


    /**
     * @notice Sender enters poll
     * @param _submarineId the ID associated 
     * Other parameters for REVEAL
     */
    function revealOption(bytes32 _submarineId, bytes _embeddedDAppData,
        string memory _witness, uint96 _commitValue ) public returns (bool success)
    {
        require(isSession(_submarineId),"Must have a valid Session ID");
        //bytes32 _commitOptionSelected is filled from a Pool of options
        for (uint i = 0; i < optionsToSelect.length ; i++){
            if (sessions[_submarineId].commitFairstamp == 0) break;
            reveal(_submarineId, _embeddedDAppData, _witness, _commitValue, optionsToSelect[i] );
            if (sessions[_submarineId].commitFairstamp == 0) {
                pollResults[_submarineId].optionSelected = optionsToSelect[i];
            }
        }
        
        if (sessions[_submarineId].commitFairstamp == 0)
        
        unlock(_submarineId, msg.sender);
        
        return revealedAndUnlocked(_submarineId);
    }

    /**
     * @notice Consumers of this library should implement their custom reveal
     *         logic by overriding this method. This function is a handler that
     *         is called by reveal. A user calls reveal, LibSubmarine does the
     *         required submarine specific stuff, and then calls this handler
     *         for client specific implementation/handling.
     * @param  _submarineId the ID for this submarine workflow
     * @param _embeddedDAppData optional Data passed embedded within the unlock
     *        tx. Clients can put whatever data they want committed to for their
     *        specific use case - in this example, we don't need to use it so
     *        it's null.
     * @param _value amount of ether revealed
     *
     */
    function onSubmarineReveal(
        bytes32 _submarineId,
        bytes _embeddedDAppData,
        uint256 _value
    ) internal {
        // In this example, we don't store any additional value
        return;
    }

}
