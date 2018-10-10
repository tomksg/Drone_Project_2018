#include <iostream>
#include <cstdio>

#include <ros/ros.h>
#include <std_msgs/String.h>
#include <std_msgs/Float64.h>
#include <geometry_msgs/PoseStamped.h>

#include <mavros_msgs/GlobalPositionTarget.h>
// GLobal GPS initiator 

#include <mavros_msgs/CommandBool.h>
#include <mavros_msgs/SetMode.h>
#include <mavros_msgs/State.h>
#include <sensor_msgs/NavSatFix.h>

//just for abs you can erase this later with cbMove X , Y if functions
#include <cmath>
//just for mission theta traj mission drone
#include "math.h"

double r = 4.0;
double theta;
double count = 0.0;
double wn = 1.0;
double temp[3];
int laps;

//동적 배열은 포기하며 이를 활용하는것은 그냥 temp 를 사용하는것을 채택을 하는것을 추천
mavros_msgs::State              g_current_state;
geometry_msgs::PoseStamped     g_pose;
ros::Publisher local_pos_pub;
ros::Publisher global_position_publisher;
mavros_msgs::GlobalPositionTarget positionTarget;

double original_latitude = 0.0;
double original_longitude = 0.0;
double original_altitude = 0.0;
bool original_flag = false;
bool zFlag = false;

// finding out that if drone is on Mission
bool OnMission = false;
bool MovingFlag = true;
//============================== < call back functions > ==============================
void cbState(const mavros_msgs::State::ConstPtr& msg);
void cbMoveX(const std_msgs::Float64::ConstPtr& msg);
void cbMoveY(const std_msgs::Float64::ConstPtr& msg);
void cbMoveZ(const std_msgs::Float64::ConstPtr& msg);


void callbackGlobal(const sensor_msgs::NavSatFix::ConstPtr& gps);

int main(int argc, char **argv)
{
    
    ros::init(argc, argv, "offboard_node");
    ros::NodeHandle nodeHandle;

    ros::Subscriber state_sub = nodeHandle.subscribe<mavros_msgs::State>("mavros/state", 10, cbState);
    ros::Subscriber global_position = nodeHandle.subscribe<sensor_msgs::NavSatFix>("mavros/global_position/global", 10, callbackGlobal);
    ros::Subscriber gpsX_sub = nodeHandle.subscribe<std_msgs::Float64>("gps_serviceX", 10, cbMoveX);
    ros::Subscriber gpsY_sub = nodeHandle.subscribe<std_msgs::Float64>("gps_serviceY", 10, cbMoveY);
    ros::Subscriber gpsZ_sub = nodeHandle.subscribe<std_msgs::Float64>("gps_serviceZ", 10, cbMoveZ);

    local_pos_pub    = nodeHandle.advertise<geometry_msgs::PoseStamped>("mavros/setpoint_position/local", 10);
    global_position_publisher = nodeHandle.advertise<mavros_msgs::GlobalPositionTarget>("mavros/setpoint_raw/global", 10);
    ros::ServiceClient  arming_client   = nodeHandle.serviceClient<mavros_msgs::CommandBool>("mavros/cmd/arming");
    ros::ServiceClient  set_mode_client = nodeHandle.serviceClient<mavros_msgs::SetMode>("mavros/set_mode");

    //the setpoint publishing rate MUST be faster than 2Hz ex) Rate rate(20,0);
    ros::Rate rate(20.0);

    // wait for FCU connection
    while(ros::ok() && g_current_state.connected){
        ros::spinOnce();
        rate.sleep();
    }
// 18 -> geometry_msgs::PoseStamped     g_pose; 에서 갖고옴 

   g_pose.pose.position.x = 0;
   g_pose.pose.position.y = 0;
   g_pose.pose.position.z = 2;

//    Ignoring lat, long, alt ?
//    std::cout << "positionTarget.IGNORE_LATITUDE: " << positionTarget.IGNORE_LATITUDE << std::endl;

    // positionTarget.IGNORE_LATITUDE = 0;
    // positionTarget.IGNORE_LONGITUDE = 0;
    // positionTarget.IGNORE_ALTITUDE = 0;

    
    
    //send a few setpoints before starting
    for(int i = 100; ros::ok() && i > 0; --i)
    {
        local_pos_pub.publish(g_pose);
        ros::spinOnce();
        rate.sleep();
    }

//============================== < initiating offb_set_mode > ==============================
 
    mavros_msgs::SetMode offb_set_mode;
    offb_set_mode.request.custom_mode = "OFFBOARD";
    
    mavros_msgs::CommandBool arm_cmd;
    arm_cmd.request.value = true;

    ros::Time last_request = ros::Time::now(); 



//============================== < Dose not need to set offb_set_mode > ==============================
    while(ros::ok()) //시작하자마자 on 시키는 방법 & ros 실행 (매 20 hz 마다))
    {
        if( g_current_state.mode != "OFFBOARD" &&
            (ros::Time::now() - last_request > ros::Duration(5.0))) //Duration 5 초는 시작하기전 delay
        {
            if( set_mode_client.call(offb_set_mode) &&
                offb_set_mode.response.success)
            {
                ROS_INFO("Offboard enabled");
            }

            last_request = ros::Time::now();
        }
	else
        {
	    if( !g_current_state.armed &&
                (ros::Time::now() - last_request > ros::Duration(5.0)))
            {
                if( arming_client.call(arm_cmd) &&
                    arm_cmd.response.success)
                {
                    ROS_INFO("Vehicle armed");
                }

                last_request = ros::Time::now();
            }
        }

        // I don't get what this dose
        // 50 -> global_position_publisher = nodeHandle.advertise<mavros_msgs::GlobalPositionTarget>("mavros/setpoint_raw/global", 10);
        global_position_publisher.publish(positionTarget);
        // didn't changed the settings

        if(MovingFlag){ //init Move
            local_pos_pub.publish(g_pose);
            temp[0] = g_pose.pose.position.x;
            temp[1] = g_pose.pose.position.y;
            temp[2] = g_pose.pose.position.z;
            MovingFlag = false;
        }
        
        if(OnMission){
            theta = wn*count*0.05;
            // std::cout << "\ncurr theta = " << theta << std::endl;
            g_pose.pose.position.x = temp[0] + r*sin(theta);
            g_pose.pose.position.y = temp[1] + r*cos(theta);
            local_pos_pub.publish(g_pose);
            count++;
            laps = theta / 360;
            if(laps>1){
                OnMission = false;
                g_pose.pose.position.x = 0;
                g_pose.pose.position.y = 0;
                g_pose.pose.position.z = 2;
                local_pos_pub.publish(g_pose);
                count = 0;
            }
        }else{
            if(laps>1){
                count++;
            }
            if(count>100){
                std::cout << "\n Finished Mission";
    
                ros::shutdown();
                
                std::cout << "\n Ending ROS";
    
            }
        }



        ros::spinOnce();
        rate.sleep();
    }

    return 0;
}

void cbState(const mavros_msgs::State::ConstPtr& msg)
{
    g_current_state = *msg;

    std::cout << "\n ========================== [blockchaindrone] ==========================";
    std::cout << "\n g_current_state.connected = " << ((g_current_state.connected) ? "OK!" : "Not yet!");
    std::cout << "\n g_current_state.armed = " << ((g_current_state.armed ) ? "OK!" : "Not yet!");
    std::cout << "\n g_current_state.guided = " << ((g_current_state.guided) ? "OK!" : "Not yet!");
    std::cout << "\n g_current_state.mode = " << g_current_state.mode<<std::endl;

    std::cout << "\n Is Drone on Mission? = " << ((OnMission) ? "Mission On" : "Mission Off") <<std::endl;

    std::cout << "\n[ORIGIN GPS] latitude: " << original_latitude << std::endl;
    std::cout << "[ORIGIN GPS] longitude: " << original_longitude << std::endl;
    std::cout << "[ORIGIN GPS] altitude: " << original_altitude << std::endl;
    std::cout << "[DSTNAT GPS] latitude: " << positionTarget.latitude << std::endl;
    std::cout << "[DSTNAT GPS] longitude: " << positionTarget.longitude << std::endl;
    std::cout << "[DSTNAT GPS] altitude: " << positionTarget.altitude << std::endl;

    //================================ < 참고할것 > ===============================
    // g.pose 는 드론이 가야할 위치를 의미하는데 이는 postionTarget 에 주는 변수를 보면 알겠지만 
    // 이는 드론 (gps) 와 입력받은 위치값의 차이다 
    
    std::cout << "\n[현재 드론의 Pos.x] g_pose.pose.position.x = " << g_pose.pose.position.x; 
    std::cout << "\n[현재 드론의 Pos.y] g_pose.pose.position.y = " << g_pose.pose.position.y;
    std::cout << "\n[현재 드론의 Pos.z] g_pose.pose.position.z = " << g_pose.pose.position.z;
    
    std::cout << "\n 드론의 바퀴수 = " << laps <<"\n Theta = "<< theta <<std::endl;

    std::cout << "\n ========================== [blockchaindrone] ==========================\n";
}

void cbMoveX(const std_msgs::Float64::ConstPtr& msg)
{
  OnMission = true;
  MovingFlag = true;
  std::printf("[message] Mission init ON\n");   
  std::printf("[message] X: %.20lf\n", msg->data);
  
// ===========================< 이 if 문은 분해해도 됩니다 10 넘어가면 자동으로 10으로 조정 >=====================
  if(std::abs(msg->data - original_latitude)>10){
      g_pose.pose.position.x = (10);
      std::printf("pose.postion.x was over 10 \n");
  }else{
      g_pose.pose.position.x = (msg->data - original_latitude);// * 10e-5;
  }
  
  // std::printf("callbackX: %.20lf\n", g_pose.pose.position.x);
//   local_pos_pub.publish(g_pose);
  positionTarget.latitude = msg->data ;
//   global_position_publisher.publish(positionTarget); // local_pos_pub.publish(g_pose);
}

void cbMoveY(const std_msgs::Float64::ConstPtr& msg)
{
  std::printf("[message} Y: %.20lf\n", msg->data);

  // ===========================< 이 if 문은 분해해도 됩니다 10 넘어가면 자동으로 10으로 조정 >=====================
  if (std::abs(msg->data - original_longitude)>10){
      g_pose.pose.position.y = (10);
      std::printf("pose.postion.y was over 10 \n");
  }else{
    g_pose.pose.position.y = (msg->data - original_longitude);// * 10e-5;
  }
  
  // std::printf("callbackY: %.20lf\n", g_pose.pose.position.y);
//   local_pos_pub.publish(g_pose);
  positionTarget.longitude = msg->data ;
//   global_position_publisher.publish(positionTarget); // local_pos_pub.publish(g_pose);
}

void cbMoveZ(const std_msgs::Float64::ConstPtr& msg)
{
  std::printf("[message] Z: %.20lf\n", msg->data);
  std::printf("callbackZ: %.20lf\n", g_pose.pose.position.z);
}

void callbackGlobal(const sensor_msgs::NavSatFix::ConstPtr& gps) //변환함수 위도 경도 -> 좌표계 + z 는 고정  참고로 GPS는 항상 수신중
// NavSatFix? -> ???
{
  //just once excute funciton original_flag was 0
  
  if (!original_flag) { 
    std::cout << "[GLOBAL -> ORIGINAL GPS SET]" << std::endl;
    original_flag = true;
    original_latitude = gps->latitude;
    original_longitude = gps->longitude;
    original_altitude = gps->altitude;
  }

 //마찬가지로 just once excuted

  if (g_current_state.mode == "OFFBOARD" && !zFlag) {
      zFlag = true;
      // g_pose.pose.position.z = original_altitude = gps->altitude;// * 10e-5;
      // local_pos_pub.publish(g_pose);
      std::cout << "Original Z: " << gps->altitude << std::endl;
      positionTarget.altitude = 0; //18500;//gps->altitude; // msg->data; it was 3e7 but i changed it
      //global_position_publisher.publish(positionTarget); // local_pos_pub.publish(g_pose);
  }

}
