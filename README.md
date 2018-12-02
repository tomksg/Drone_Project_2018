# Drone_Project_2018
Graduation thesis 

thx to Ethereum Drone Employee Project for the concept of Ethereum + Drone

기존 인터페이스는 GSDemo-github DJI Doc 참조



이벤트 처리
Infura에서는 filter를 지원하지 않으므로 이벤트를 통한 동작 제어가 불가능하다.
이를 해결하기 위해서는 두 가지 방법이 있다.
컨트랙트 내 데이터를 수시로 체크하는 방법과 어플리케이션을 실행시킬 디바이스가 이더리움 노드가 되는 방법이 있다.
하지만 후자의 경우에는 많은 하드웨어적 리소스를 필요로 하므로 데이터를 수시로 체크하는 방법을 선택한다.

Github subModule 참조
git clone --recurse-submodules *git clone 대상* 으로 시행해줘야지 submodule 을 성공적으로 받아올 수 있다.
