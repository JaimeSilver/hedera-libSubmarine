//0x0000000000000000000000000000000000000000000000000000000000000000

pragma solidity ^0.4.25;

import "./hashgraphSubmarine.sol";
import "./SafeMath.sol";

/**
 * Example Hidden contract for guessing a 2 digit code paying a reward
 *
 * Note: *********** PLEASE DONT USE THIS CODE IN PRODUCTION ***************
 * this contract is just an example of how hasgraphSubmarine can be used.
 * The code has not been designed to be efficient, performant, or secure,
 * only easy to understand.
 * This code has multiple problems.
 * it is JUST AN EXAMPLE.
 */
contract theGuessingGame is hashgraphSubmarine {
    using SafeMath for uint256;

    //
    // STORAGE
    //

    address owner;
    
    struct DataStruct {
        bytes32 primaryKey;
        uint index;
    }
  
    mapping(uint256 => DataStruct) internal gameRounds;
    uint256[] internal gameIndex;
    event LogNewGame   (address indexed userAddress, uint index, uint256 gameSession);

    /// MODIFIERS
    modifier onlyOwner() {
        require(msg.sender == owner);
        _;
    }

    /**
     * @notice Constructor, creates the Contract.
     */
    constructor( ) public {
        owner = msg.sender;
        minRevealTimestamp = now;
    }

    /**
     * @notice Validate if session exists
     * @param _gameId the ID associated 
     */
    function isGame(uint256 _gameId)
        public view returns(bool isIndeed) 
    {
        if(gameIndex.length == 0) return false;
        return (gameIndex[gameRounds[_gameId].index] == _gameId);
    }

    /**
     * @notice Validate if session exists
     * @param _gameId the ID associated 
     */
    function insertGame(
        uint256 _gameId,
        bytes32 _submarineId) 
        private
        returns(uint index)
    {
        if(isGame(_gameId)) revert();
        //Nothing is recorded; have to wait until Reveal Occurs
        gameRounds[_gameId].primaryKey = _submarineId;
        gameRounds[_gameId].index  = gameIndex.push(_gameId)-1;
        emit LogNewGame( msg.sender, gameRounds[_gameId].index,  _gameId);
        return gameIndex.length-1;
    }
    
    
    /**
     * @notice Sender enters gameID
     * @param _gameId the ID associated 
     * @param _submarineId the ID associated 
     * @param _secondId the Second ID associated
     */
    function registry(uint256 _gameId, bytes32 _submarineId, bytes32 _secondId, uint256 _value ) public returns (uint index)
    {
        require( msg.sender != tx.origin, "Function cannot be called directly" ); 
        index = insertGame(_gameId, _submarineId);
        sessions[_submarineId].amount = uint96(_value);
        //With this option, the transfer is managed at the Runner Contract, not in the unlock function
        sessions[_submarineId].amountUnlocked = uint96(_value);
        sessions[_submarineId].commitFairstamp = block.timestamp;
        sessions[_submarineId].secondKey = _secondId;
        return index;
    }

     /**
     * OVERRIDE
     * @notice No reads are available
     * @param _submarineId session ID
     * @return 0 always
     */
    
    function getSubmarineState(bytes32 _submarineId) public view returns (
        uint96 amount,
       uint96 amountUnlocked,
        uint commitFairstamp,
        bytes32 secondKey ) {
        return (0, 0, 0, bytes32(0) );
    }
     /**
     * OVERRIDE
     * @notice No reads are available
     * @param _submarineId session ID
     * @return 0 always
     */
    function getSubmarineAmount(bytes32 _submarineId) public view returns (
        uint96 amount
    ) {
        return 0;
    }
    /**
     * @notice Sender enters gameID
     * @param _gameId the ID associated 
     * Other parameters for REVEAL
     */
    function revealOption(uint256 _gameId, bytes _embeddedDAppData,
        string memory _witness, uint96 _commitValue ) public returns (bool success)
    {
        require(isGame(_gameId),"Must have a valid Game ID");
        require( msg.sender != tx.origin, "Function cannot be called directly" );
        require(!revealedAndUnlocked(gameRounds[_gameId].primaryKey), "Transaction already revealed");
     
        //Catch the error before the revert; because it cancels the collection of money
        bytes32 secondKey = getSecondKey( msg.sender, address(this), _witness );
        if ( sessions[gameRounds[_gameId].primaryKey].secondKey != secondKey ) return false;

        reveal(gameRounds[_gameId].primaryKey, _embeddedDAppData, _witness, _commitValue, bytes32(0) );

        if (sessions[gameRounds[_gameId].primaryKey].commitFairstamp == 0)
            unlock(gameRounds[_gameId].primaryKey, msg.sender);
        
        return revealedAndUnlocked(gameRounds[_gameId].primaryKey);
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

/**
 * Example contract to provide guesses and pay Hbars in return
 *
 * Note: *********** PLEASE DONT USE THIS CODE IN PRODUCTION ***************
 * this contract is just an example of how hasgraphSubmarine can be used.
 * The code has not been designed to be efficient, performant, or secure,
 * only easy to understand.
 * This code has multiple problems! It is JUST AN EXAMPLE.
 */
contract theRunnerContract {
    using SafeMath for uint256;
    //
    // STORAGE
    //
    address owner;
    theGuessingGame guessingGame;
    uint256 payHbars;
    uint256 openHbars;
    
    /// MODIFIERS
    modifier onlyOwner() {
        require(msg.sender == owner);
        _;
    }

    /**
     * @notice Constructor, creates the Contract.
     */
    constructor( ) public {
        owner = msg.sender;
        // Pays 1 Hbar
        payHbars = 100000000;
        
        // 5 Hbar
        openHbars = 500000000;

        guessingGame = new theGuessingGame();
    }
 
    //
    // GETTERS
    //

    /**
     * @notice Read if the GameID is valid
     * @param _gameId Game ID 
     */

    function isGame(uint256 _gameId)
        public view returns(bool isIndeed) 
    {
        return guessingGame.isGame(_gameId);
    }

    /**
     * @notice Option to read the contract balance
     */

	function getBalance() public view returns (uint256 _balance){
		return address(this).balance;
	}

    /**
     * @notice Getter to read the getSubmarineAddress
     */
	function getSubmarineAddress() public view returns (address _guessingGame){
		return address(guessingGame);
	}    

    //
    // SETTERS
    //

    /**
     * @notice Sender enters gameID
     * @param _gameId the ID associated 
     * @param _submarineId the ID associated 
     * @param _secondId the Second ID associated
     */
    function registry(uint256 _gameId, bytes32 _submarineId, bytes32 _secondId ) public payable returns (uint index)
    {
        require(msg.value == openHbars, "Must deposit 5 Hbars");
        return guessingGame.registry(_gameId, _submarineId, _secondId,  msg.value);
    }

    /**
     * 
     * @notice Sender enters gameID
     * @param _gameId the ID associated 
     * Other parameters for REVEAL
     */
    function revealOption(uint256 _gameId, bytes _embeddedDAppData,
        string memory _witness, uint96 _commitValue ) public payable returns (bool success)
    {
        //Convert into a payable address
        require(msg.value == payHbars, "Must deposit 1 Hbar");

        // Success revealing the model.
        if (guessingGame.revealOption(_gameId,_embeddedDAppData, _witness, _commitValue)){
            address(tx.origin).transfer(openHbars);
            return true;
        } else {
            return false;
        }
    }

}