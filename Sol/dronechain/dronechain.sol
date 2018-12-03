pragma solidity ^0.4.25;
contract Dronechain {
    
    /*
    state 값에 따른 상태
    0 : 컨트랙트 상에 등록 완료
    1 : 드론 수신 완료
    2 : 미션 진행 중
    3 : 미션 끝
    */
    struct mission{
        int256[] dstLat;
        int256[] dstLong;
        address commander;
        uint8 state;
    }
    struct drone {
        int256 droneLat;
        int256 dronelong;
        mission[] missions;
        uint32 amountOfWaypoint;
        uint8 state;//
    }
    mapping (address => drone)  drones;
    address[] droneList;
    event createMission(address indexed _from, address indexed _to, uint256 _index);
    event updateMissionState(address indexed _from, address indexed _to, uint256 _index);
    
    
    /*
    드론에게 웨이포인트를 지정하는 함수
    - 입력 설명
    _drone : 웨이포인트를 지정할 드론의 주소
    _latitude : 웨이포인트의 위도 값
    _longitude : 웨이포인트의 경도 값
    - 반환 설명
    _drone : 미션을 신청한 드론의 주소.
    _drones[_drone].missions.length-1 : 미션 배열의 인덱스로 신청한 미션의 인덱스 값.
    *-- 웨이포인트를 부여했을 때 해당 내용을 이벤트로 발생시켜서 드론에게 전송 이때, 
    드론은 drones[_drone].missions.length-1 값을 missions 의 인덱스로 사용해 명령에 바로 접근 할 수 있다. --*
    */
    function setWayPoint(address _drone, int256[] _latitude, int256[] _longitude) external returns(address droneAddr,uint256 cmdIndex){
        //check range
        drones[_drone].missions.push(mission(_latitude,_longitude,msg.sender,0));
        drones[_drone].amountOfWaypoint = drones[_drone].amountOfWaypoint + (uint32)(_latitude.length);
        emit createMission(msg.sender, _drone,drones[_drone].missions.length-1);
        return(_drone,drones[_drone].missions.length-1 );
    }
    
    
    /*
    setWayPoint 의 반환 값으로 신청한 미션의 진행상황을 파악하기 위한 함수다.
    - 입력 설명
    _drone : 드론의 주소
    _index : 명령 배열에서 참조할 인덱스 값
    - 반환 설명c
    drones[_drone].missions[_index].dstLat : 신청한 미션의 위도 값
    drones[_drone].missions[_index].dstLong : 신청한 미션의 경도 값
    drones[_drone].missions[_index].state : 진행 상황을 나타낸 값
    */
    function getMission(address _drone, uint256 _index) external view returns(int256[] _lat, int256[] _long, uint8 _state){
        require((drones[_drone].missions[_index].commander == msg.sender) || (_drone == msg.sender));
        return(drones[_drone].missions[_index].dstLat, drones[_drone].missions[_index].dstLong, drones[_drone].missions[_index].state);
    }
    
    
    /*
    드론으로 등록하려는 함수
    - 입력 설명
    _latitude : 현재 드론의 위도 값
    _longitude : 현재 드론의 경도 값
    */
    function registerDrone(int256 _latitude, int256 _longitude) external{
        droneList.push(msg.sender);
        drones[msg.sender].droneLat =_latitude;
        drones[msg.sender].dronelong = _longitude;
        drones[msg.sender].state = 0;
    }
    
    /*
    드론의 비행이력을 조회하는 함수
    - 입력 설명
    _drone : 조회하고자 하는 드론의 어드레스
    _state : 조회하고자 하는 이력의 상태값
    - 반환 설명
    * 입력 조건에 해당하는 값들의 배열이다. 첫 번째 값을 표시하려면 dstLat[0], dstLong[0], commander[0] 과 같이 사용하면 된다.*
    dstLat : 입력 조건에 해당하는 위도 값
    dstLong : 입력 조건에 해당하는 경도 값
    commander : 입력 조건에 해당하는 지시자 주소
    cnt : 총 index의 수
    */
    function traceFlightHistory(address _drone,uint8 _state) external view returns(int256[] , int256[] , address[] , uint ){
        uint32 cnt = 0;
        int256[] memory dstLat = new int256[](drones[_drone].amountOfWaypoint);
        int256[] memory dstLong = new int256[](drones[_drone].amountOfWaypoint);
        address[] memory commander = new address[](drones[_drone].amountOfWaypoint);
        for(uint32 i = 0; i<drones[_drone].missions.length; i++){
            for(uint32 j = 0; j<drones[_drone].missions[i].dstLat.length; j++)
            if(drones[_drone].missions[i].state == _state){
                dstLat[cnt] = drones[_drone].missions[i].dstLat[j];
                dstLong[cnt] = drones[_drone].missions[i].dstLong[j];
                commander[cnt] = drones[_drone].missions[i].commander;
                cnt++;
            }
        }
        return(dstLat,dstLong,commander,cnt);
    }
    
    
    /*
    현재 컨트랙트에 등록된 드론의 리스트를 반환하는 함수
    - 반환 설명
    droneList : registerDrone 함수를 통해 형성된 리스트며 현재 컨트랙트에 등록된 드론 어드레스 배열이다
    */
    function getDrones() external view returns(address[]){
        return(droneList);
    }
    
    
    
    /*
    드론의 주소를 통해 해당 드론의 상태 값들(위도, 경도, 상태)을 조회하는 함수다.
    - 입력 설명
    _drone : 조회하고자 하는 드론의 주소
    - 반환 설명
    drones[_drone].droneLat = 해당 드론의 위도 값
    drones[_drone].dronelong = 해당 드론의 경도 값
    drones[_drone].state = 해당 드론의 현재 상태 값
    */
    function getDroneStateByAddr(address _drone) external view returns(int256, int256, uint8){
        return(drones[_drone].droneLat, drones[_drone].dronelong, drones[_drone].state);
    }
    
    
    
    /*
    updateDronePos function
    msg.sender(함수를 호출하는 드론의 주소)의 위치를 갱신하기 위한 함수
    입력 설명
    _latitude : 위도
    _longitude : 경도
    *--위도와 경도값은 소수점 밑 여섯 자리까지 표현해야 하지만 솔리디티에서 float타입을 지원하지 않기 때문에 10^6을 곱한 값으로 인자 전달--*
    */
    function updateDronePos( int32 _latitude, int32 _longitude) external{
        drones[msg.sender].droneLat = _latitude;
        drones[msg.sender].dronelong = _longitude;
    }

    
    /*
    updateState function
    드론이 현재 수행중인 미션에 대한 상태를 갱신하고 해당 내용을 commander에게 알리기 위해 이벤트 발생
    - 입력 설명
    _index : missions배열 중 _index번째 미션의 state를 _state값으로 변경
    _state : 변경할 state 값
    */
    function updateState(uint32 _index, uint8 _state) external{
        drones[msg.sender].missions[_index].state = _state;
        emit updateMissionState(msg.sender, drones[msg.sender].missions[_index].commander,_state);
    }
    /*
    returnMissionLength
    드론에 신청된 미션의 크기를 파악하기 위한 함수
    - 반환 설명
    drones[msg.sender].missions.length : msg.sender 드론의 missions 의 크기를 반환한다.
    */
    function returnMissionLength()external view returns(uint256){
        return(drones[msg.sender].missions.length);
    }
}
