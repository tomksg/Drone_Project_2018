pragma solidity ^0.4.21;

contract Auction{

	event NewRecentBidder(uint RecentBidderId, string name, uint Bid); // js 에서 이벤트처리
	event highestBidIncreased(address bidder , uint amount);

	mapping(address => uint) MoneyReturns; // 돌려줘야하는 최고가 

	uint public highestBid; // 최고입찰자
	address public highestBidder; // 최고입찰자 주소

    struct RecentBidder { // 경쟁자 목록 
        string name;
        uint bid;
	}
	
	RecentBidder[] public Bidder; // 최근 입찰한 경쟁자들의 목록

	function bid(string _name, uint _Bid) public payable{
		uint id = Bidder.push(RecentBidder(_name,_Bid))-1;
		emit NewRecentBidder(id, _name, _Bid); // js에서 맨 마지막 Bidder 를 부르는 방식을 사용하면 될것같아보입니다.
		
		if(msg.value > highestBid){
			if (highestBid !=0){ //누군가 이미 입찰을 진행함 이경우 입찰한 금액을 전 입찰자에게 보내줘야함
				MoneyReturns[highestBidder] += highestBid;
			}
			highestBidder = msg.sender; // 다시 최고가 입찰자를 설정
			highestBid = msg.value;
			emit highestBidIncreased(msg.sender, msg.value); // 최고 입찰자가 변경되었음을 알림
		}
	}

	function withdraw() public returns (bool){
		
		uint amount = MoneyReturns[msg.sender]; //amount 는 msg.sender 에게 보내줄 값

		if(amount>0){ // 돌려줄 값이 0 이상이라면.
			MoneyReturns[msg.sender] = 0; // 바로 돌려주는것이아닌 실제 돌려받을사람이 돌려받기를 원해야 지급하는방식
			if(!msg.sender.send(amount)){ //이 함수가 fail 할경우 throw 말고 amount 를 다시 입력
				MoneyReturns[msg.sender] = amount;
				return false;
			}
		}
		return true;
	}

}