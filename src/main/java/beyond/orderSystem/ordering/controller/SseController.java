package beyond.orderSystem.ordering.controller;
import beyond.orderSystem.ordering.dto.OrderListResDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class SseController implements MessageListener {
    // SseEmiter는 연결된 사용자 정보를 의미 ( 연결된 사용자 정보가 무슨 의미?? 그 사용자의 IP 주소, 위치 정보, 웹 브라우저 정보 이런것들이 담겨 있음 )
    // ConcurrentHashMap는 Thread-safe한 map(동시성 이슈 발생 안 함) - 동시에 와가지고 데이터를 집어 넣는 그런 작업이 안됨
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>(); // String 에는 이메일 정보를 넣을 거임

    // 여러번 구독을 방지 하기 위한 ConcurrentHashSet 변수 생성.
    private Set<String> subscribeList = ConcurrentHashMap.newKeySet();

    @Qualifier("4")
    private final RedisTemplate<String,Object> sseRedisTemplate;

    private final RedisMessageListenerContainer redisMessageListenerContainer;

    public SseController(@Qualifier("4")RedisTemplate<String, Object> sseRedisTemplate, RedisMessageListenerContainer redisMessageListenerContainer) {
        this.sseRedisTemplate = sseRedisTemplate;
        this.redisMessageListenerContainer = redisMessageListenerContainer;
    }

    // email에 해당되는 메시지를 listen 하는 listener를 추가한 것.
    public void subscribeChannel(String email){
        // 이미 구독한 email일 경우에는 더이상 구독하지 않는 분기처리
        if(!subscribeList.contains(email)){
            MessageListenerAdapter listenerAdapter = createListenerAdapter(this);
            redisMessageListenerContainer.addMessageListener(listenerAdapter, new PatternTopic(email));
            subscribeList.add(email); // admin@test.com 란 이메일을 구독 목록에 넣을테니 만약 admin@test.com 이란 메시지로 또 구독 요청이 오면 무시하겠따

        }

    }

    private MessageListenerAdapter createListenerAdapter(SseController sseController){
        return new MessageListenerAdapter(sseController, "onMessage");
    }

    // A 사용자 서버가 있다고 가정하면 서버에서 A로 알람을 쏴주기 위한 전제조건은 연결을 먼저 맺어야 한다.
    // 그럼 A 사용자 입장에서 그럼 나 연결 맺어줘 이따가 서버한테 알람을 받고 싶어
    // 그러면 서버 입장에서는 GetMapping 으로 연결 할 수 있는 루트를 하나 만들어줘야 한다.
    @GetMapping("/subscribe")
    public SseEmitter subscribe(){
        SseEmitter emitter = new SseEmitter(14400*60*1000L); // (emitter 정보가 유효한 )유효 시간을 30분 정도로 emitter 유효시간 설정
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication(); // 사용자 정보는 Authentication 객체 안에 들어가 있음.
        String email = authentication.getName();
        emitters.put(email,emitter); // 누가 서버한테 나 구독하겠다 라고 요청하면 그 사용자 정보가 emitter 객체이다
                                     // 이 emitter 객체를 emitters 에다가 add 해준다. ( emitters 는 서버 입장에서는 그 사용자가 매우 많으니깐 그 사용자 마다의 연결정보를 다 가지고 있는 것 )
                                     // email 을 key 로 잡아서 emitters 에다가 put 해준다.
        emitter.onCompletion(()->emitters.remove(email)); // 작업이 끝나면 emitter를 emitters 에서 remove 시켜주겠다.
        emitter.onTimeout(()->emitters.remove(email)); // 유효 시간이 다 지나면 서버에서 emitter를 제거해버리겠다.
        try{
            // 사용자한테 이제 응답을 주겠다.
            // eventName : 이벤트 이름, 메세지 카테고리 /  object : 실질적인 메세지
            emitter.send(SseEmitter.event().name("connect").data("connected!!")); // 연결을 요청한 그 emitter 한테 너 연결 맺어졌어 라고 응답을 주는 코드
        }catch (IOException e){
            e.printStackTrace();
        }
        subscribeChannel(email); // redis 에서 subscribe 하겠다
        return emitter;
    }
    // 여기는 emitters 라는 서버가 관리하는 사용자 목록에 등록되기 위한 작업임.
    // 그럼 프론트 쪽에서 나 등록해줘 라는 요청이 있어야 함
    // 그러고 등록이 되면 서버는 너 등록됐어 라고 connected 라고 하면서 응답을 줌

    public void publishMessage(OrderListResDto dto, String email){
        SseEmitter emitter = emitters.get(email);
        // emitter 있으면 내가 처리
        // redis pub/sub 실습 테스트를 위해 잠시 주석처리
//        if(emitter !=null){
//            try {
//                emitter.send(SseEmitter.event().name("ordered").data(dto));
//            }catch(IOException e){
//                throw new RuntimeException(e);
//            }
//        }
//        //emitter 없으면 레디스에 배포(노션 sse 알림 노트 보면 좀 알거야)
//        else{
//            // redisconfig에 4번 qualifier야!
//            // convertAndSend : 직렬화해서 보내겠다는 것
        sseRedisTemplate.convertAndSend(email,dto);
//        }

    }


//    사용자가 주문을 한다.
//    = 사용자와 연결되어 있는 서버가 레디스로 주문 정보를 보낸다.
//    = 레디스의 DB 중에서 admin@test.com 가지고 있는 구독되어 있는 서버에 메시지를 보낸다
    @Override
    public void onMessage(Message message, byte[] pattern) {
//        아래는 message 내용 parsing 해주는 것
        ObjectMapper objectMapper  = new ObjectMapper();
        try {
            OrderListResDto dto =objectMapper.readValue(message.getBody(), OrderListResDto.class);

            String email = new String(pattern, StandardCharsets.UTF_8);
            SseEmitter emitter = emitters.get(email);
            if(emitter != null){
                emitter.send(SseEmitter.event().name("ordered").data(dto));
            }
            System.out.println("listening");
            System.out.println(dto);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

