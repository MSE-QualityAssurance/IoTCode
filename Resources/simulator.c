/* simulator.c
 *
 * Copyright (c) 2000 Sean Walton and Macmillan Publishers.  Use may be in
 * whole or in part in accordance to the General Public License (GPL).
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

/*****************************************************************************
 A simple client server simulator to communicate
 with the IoT House
*****************************************************************************/

#include <unistd.h>
#include <stdio.h>
#include <time.h>
#include <errno.h>
#include <string.h>
#include <stdlib.h>
#include <sys/socket.h>
#include <resolv.h>
#include <arpa/inet.h>
#include <errno.h>

#define MY_PORT		5050
#define MAXBUF		1024

char * HandleGetState();
char * HandleSetState(char *newState);

int GetTemp();
int GetHumidity();
int GetDoorState();
int GetHumdifierState();
int GetLightState();
int GetProximityState();
int GetAlarmState();

int Temp = -1;            // TR
int Humidity = -1;        // HR
int DoorState = 0;        // DS
int LightState = 0;       // LS
int ProximityState = 0;   // PS
int AlarmState = 0;       // AS

/**
 * Execution starts here.
 */
int main(int Count, char *Strings[]) {
   
   int sockfd;
   struct sockaddr_in self;
   char buffer[MAXBUF];
   
   /*---Create streaming socket---*/
   if ( (sockfd = socket(AF_INET, SOCK_STREAM, 0)) < 0 )
   {
      perror("Socket");
      exit(errno);
   }

   /*---Initialize address/port structure---*/
   bzero(&self, sizeof(self));
   self.sin_family = AF_INET;
   self.sin_port = htons(MY_PORT);
   self.sin_addr.s_addr = INADDR_ANY;

   /*---Assign a port number to the socket---*/
  
   if ( bind(sockfd, (struct sockaddr*)&self, sizeof(self)) != 0 )
   {
      perror("socket--bind");
      exit(errno);
   }

   /*---Make it a "listening socket"---*/
   if ( listen(sockfd, 20) != 0 )
   {
      perror("socket--listen");
      exit(errno);
   }
   printf("listening on port %u\n", MY_PORT);
   /*---forevers... ---*/
 
   int clientfd;
   struct sockaddr_in client_addr;
   unsigned int addrlen=sizeof(client_addr);
    
   /*---accept a connection (creating a data pipe)---*/
   clientfd = accept(sockfd, (struct sockaddr*)&client_addr, &addrlen);
   printf("%s:%d connected\n", inet_ntoa(client_addr.sin_addr), ntohs(client_addr.sin_port));

   size_t amt = 0, bytes_received;
   
   char c;
   while (recv(clientfd,&c,1,0) > 0) {
      printf("Byte received %c\n", c);
      if (c == '.') break;
      buffer[amt] = c;
      amt++;
            
   }
   buffer[amt] = '\0';

   
   printf("Message received: %s\n", buffer);

   
   if (buffer[0] == 'G') {

      char *state = HandleGetState();
      
      write(clientfd,state,strlen(state));
      printf("sent state %s\n", state);

      if (state) free(state);
      
   } else if (buffer[0] == 'S') {

      printf("Detected set state\n");

      char newState[1000];
      memcpy(newState,&buffer[3],amt-3);

      newState[amt-4] = '\0';

      printf("New State: %s\n", newState);
      
      if (HandleSetState(newState) == 0) {
         char *ok = "OK";
         write(clientfd,ok,strlen(ok));
      }
      else {
         printf("Unknown command\n");
      }
   }    
     
   /*---Clean up (should never get here!)---*/
   
   /* close(sockfd); */
   printf("socket closed");
   
   return 0;
}

/**
 * Handle set state messages
 */
char *HandleSetState(char *newState) {
  
   char *body[20];
   unsigned int counter = 0;
   unsigned long i=0;
   char param[10];
   int j=0;
   
   while (i < strlen(newState)) { 
      char c = newState[i];
      if (c == ';') {
         param[j+1] = '\0';
         printf("Saving param %s\n", param);
         
         // save the new param
         body[counter] = malloc(sizeof(char)*100);
         memcpy(body[counter],param,10);
         
         counter++;
         memset(param,0,10);
         j=0;
      }    
      else {
         param[j++] = c;
      }
      i++;
   }

   for (i=0; i<counter;i++) {
      char *item = body[i];

      printf("Item: %s\n", item);
      
      char newVal = item[3]; // the 3rd character will be the new value (XX=V)
      // Door
      if (item[0] == 'D') {
         if ( newVal == '1') {
            DoorState = 1;
         } else {
            DoorState = 0;
         }
      }
      // Light
      else if (item[0] == 'L') {
         if ( newVal == '1') {
            LightState = 1;
         } else {
            LightState = 0;
         }
      }
      // Alarm
      else if (item[0] == 'A') {
         if ( newVal == '1') {
            AlarmState = 1;
         } else {
            AlarmState = 0;
         }   
      }
   }
   
   // free the params
   for (i=0; i<counter;i++) {
      if (body[i]) free(body[i]);
   }
   
   return 0;
}

/**
 * Handle request for current state.
 */
char * HandleGetState() {
   
   char *state = malloc(sizeof(char) * 1000);

   // return a state update
   sprintf(state, "SU:TR=%d;HR=%d;DS=%d;LS=%d;PS=%d;AS=%d.",
           GetTemp(),
           GetHumidity(),
           GetDoorState(),
           GetLightState(),
           GetProximityState(),
           GetAlarmState());

   return state;
}

// The following methods can be used to to fetch various state
// variables. If special values need to be returned, update these
// methods.

int GetTemp() {
   return Temp;
}

int GetHumidity() {
   return Humidity;
}

int GetDoorState() {
   return DoorState;
}

int GetLightState() {
   return LightState;
}

int GetProximityState() {
   return ProximityState;
}

int GetAlarmState() {
   return AlarmState;
}
