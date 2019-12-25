pragma solidity 0.5.3;

contract simpleContract {
    address public owner;
    bool isAlive;
    event LogSetAlive(address _contract, bool _state);
    event LogChangeOwner(address _contract, address _newOwner);
    
    modifier onlyOwner{
        require(msg.sender == owner);
        _;
    }
    constructor () public {
        bool isAlive = true;
        owner = msg.sender;
    }
    
    function getIsAlive() public view returns (bool isIndeed){
        return isAlive;
    }
    
    function add(uint x, uint y) public pure returns (uint z){
        return x + y;
    }
    
    function pingBytes32(bytes32 txtIn) public pure returns (bytes32 txtOut){
        return txtIn;
    }

    function pingAddress(address addIn) public pure returns (address addtOut){
        return addIn;
    }

    function pingString(string memory strIn) public pure returns (string memory strOut){
        return strIn;
    }
    
    function pingBytes( bytes memory bytIn) public pure returns (bytes memory bytOut){
        return bytIn;
    }

    function setIsAlive(bool newState) public onlyOwner returns (bool success) {
        
        isAlive = newState;
        emit LogSetAlive(msg.sender, newState);
        return true;
    }
    function changeOwner(address newOwner) public onlyOwner returns (bool success){
        owner = newOwner;
        emit LogChangeOwner(msg.sender, newOwner);
        return true;
    }
    
}