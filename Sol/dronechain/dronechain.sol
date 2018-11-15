pragma solidity ^0.4.0;
contract DroneChain {
    struct cmd{
        int32 sixTimesLat;
        int32 sixTimeslong;
        uint8 cmdType;
        int32 sixTimesdata;
        uint fee;
        address excutor;
        uint state;
        // state = 0 -> waiting, sate = 1 -> excueting, state = 2 -> finish
    }
    address[] addressList;
    mapping (address => cmd) commands;
    event CmdFinish();
    
    //if cmd.state == 1 -> finish event
    
    //cmd upload // payable
    function createCmd(int32 lat, int32 long, uint8 cType, int32 data) external payable{
        commands[msg.sender] = cmd(lat, long, cType, data, msg.value, this,0);
        addressList.push(msg.sender);
    }
    

    //state upload 
    function stateUpdate(address _requestor, uint8 _state, int32 _data) external payable{
        //waiting -> excuting
        if(_state == 1){
            require(commands[_requestor].state == 0);
            commands[_requestor].state = _state;
            commands[_requestor].excutor = msg.sender;
        }
        //excuting -> finish
        else if(_state == 2 ){
            require(commands[_requestor].state == 1);
            commands[_requestor].state = _state;
            commands[_requestor].sixTimesdata = _data;
            msg.sender.transfer(commands[_requestor].fee);
        }

    }
    //check cmd
    function getCmdListByAddr() external view returns(address[]){
        return(addressList);
    }
    function getCmd(address _address) external view returns( int32 sixTimesLat, int32 sixTimeslong, uint8 cmdType, int32 sixTimesdata, uint fee, address excutor, uint state) {
        return(commands[_address].sixTimesLat, commands[_address].sixTimeslong, commands[_address].cmdType, commands[_address].sixTimesdata, commands[_address].fee, commands[_address].excutor, commands[_address].state);
    }
}