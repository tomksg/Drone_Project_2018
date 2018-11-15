pragma solidity ^0.4.25;
contract Dronechain {
    struct mission{
        int32 dstLat;
        int32 dstLong;
        address commander;
        uint8 state;
    }
    struct drone {
        int32 droneLat;
        int32 dronelong;
        mission[] missions;
        uint8 state;//
    }
    mapping (address => drone)  drones;
    address[] droneList;
    event createMission(address indexed _from, address indexed _to, uint256);
    
    
    /*
    드론에게 웨이포인트를 지정하는 함수
    - 입력 설명
    _drone : 웨이포인트를 지정할 드론의 주소
    _latitude : 웨이포인트의 위도 값
    _longitude : 웨이포인트의 경도 값
    *-- 웨이포인트를 부여했을 때 해당 내용을 이벤트로 발생시켜서 드론에게 전송 이때, 
    드론은 drones[_drone].missions.length-1 값을 missions 의 인덱스로 사용해 명령에 바로 접근 할 수 있다. --*
    */
    function setWayPoint(address _drone, int32 _latitude, int32 _longitude) external {
        //check range
        require(_latitude <=90000000 && _latitude >=-90000000 && _longitude <= 180000000 && _longitude >=-180000000);
        drones[_drone].missions.push(mission(_latitude,_longitude,msg.sender,0));
        emit createMission(msg.sender, _drone,drones[_drone].missions.length-1); 
    }
    
    
    /*
    드론으로 등록하려는 함수
    - 입력 설명
    _latitude : 현재 드론의 위도 값
    _longitude : 현재 드론의 경도 값
    */
    function registerDrone(int32 _latitude, int32 _longitude) external{
        require(_latitude <=90000000 && _latitude >=-90000000 && _longitude <= 180000000 && _longitude >=-180000000);
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
    function traceFlightHistory(address _drone,uint8 _state) external view returns(int32[] , int32[] , address[] , uint ){
        uint32 cnt = 0;
        int32[] memory dstLat = new int32[](drones[_drone].missions.length);
        int32[] memory dstLong = new int32[](drones[_drone].missions.length);
        address[] memory commander = new address[](drones[_drone].missions.length);
        for(uint32 i = 0; i<drones[_drone].missions.length; i++){
            if(drones[_drone].missions[i].state == _state){
                dstLat[cnt] = drones[_drone].missions[i].dstLat;
                dstLong[cnt] = drones[_drone].missions[i].dstLong;
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
    function getDroneStateByAddr(address _drone) external view returns(int32, int32, uint8){
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
    드론이 현재 수행중인 미션에 대한 상태를 갱신하는 함수
    - 입력 설명
    _index : missions배열 중 _index번째 미션의 state를 _state값으로 변경
    _state : 변경할 state 값
    */
    function updateState(uint32 _index, uint8 _state) external{
        drones[msg.sender].missions[_index].state = _state;
    }
}
