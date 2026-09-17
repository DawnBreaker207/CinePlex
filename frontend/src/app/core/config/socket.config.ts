import { RxStompConfig } from '@stomp/rx-stomp';
import SockJS from 'sockjs-client';

export const SocketConfig: RxStompConfig = {
  brokerURL: '',
  webSocketFactory: () => new SockJS('http://localhost:8888/ws'),

  reconnectDelay: 5000,
  heartbeatIncoming: 0,
  heartbeatOutgoing: 20000,

  debug: () => {},

  connectHeaders: {},

  beforeConnect: () => {},
};
