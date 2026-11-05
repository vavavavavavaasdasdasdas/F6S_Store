package com.example.demodatn.config;

import com.example.demodatn.controller.user.SupportChatApiController;
import com.example.demodatn.service.hotro.SupportChatService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.*;
import org.springframework.http.server.*;
import org.springframework.messaging.*;
import org.springframework.messaging.simp.config.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.messaging.support.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.messaging.web.csrf.XorCsrfChannelInterceptor;
import org.springframework.security.messaging.web.socket.server.CsrfTokenHandshakeInterceptor;
import org.springframework.web.socket.*;
import org.springframework.web.socket.config.annotation.*;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;
import java.util.*;
import java.util.concurrent.*;

@Configuration @EnableWebSocketMessageBroker @RequiredArgsConstructor
public class SupportChatSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final SupportChatService chat;
    private final Map<String,SupportChatService.Actor> actors=new ConcurrentHashMap<>();
    private final Map<String,jakarta.servlet.http.HttpSession> sessions=new ConcurrentHashMap<>();
    @Override public void configureMessageBroker(MessageBrokerRegistry registry){registry.enableSimpleBroker("/topic/support");registry.setApplicationDestinationPrefixes("/app");}
    @Override public void registerStompEndpoints(StompEndpointRegistry registry){
        registry.setErrorHandler(new StompSubProtocolErrorHandler(){@Override public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> client,Throwable error){var h=StompHeaderAccessor.create(StompCommand.ERROR);h.setMessage("Không có quyền hoặc không thể xử lý kết nối chat.");return MessageBuilder.createMessage(new byte[0],h.getMessageHeaders());}});
        registry.addEndpoint("/ws/support").addInterceptors(new CsrfTokenHandshakeInterceptor(),new HandshakeInterceptor(){
            @Override public boolean beforeHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Map<String,Object> attrs){
                if(!(request instanceof ServletServerHttpRequest servlet))return false;
                try{var actor=SupportChatApiController.actor(servlet.getServletRequest());if(!chat.identity(actor).staff()&&chat.current(actor).isEmpty())return false;attrs.put("supportActor",actor);attrs.put("supportHttpSession",servlet.getServletRequest().getSession(false));return true;}
                catch(RuntimeException denied){response.setStatusCode(org.springframework.http.HttpStatus.FORBIDDEN);return false;}
            }
            @Override public void afterHandshake(ServerHttpRequest request,ServerHttpResponse response,WebSocketHandler handler,Exception error){}
        }).withSockJS().setSessionCookieNeeded(true);
    }
    @Override public void configureClientInboundChannel(ChannelRegistration registration){
        registration.interceptors(new XorCsrfChannelInterceptor(),new ChannelInterceptor(){
            @Override public Message<?> preSend(Message<?> message,MessageChannel channel){
                var h=MessageHeaderAccessor.getAccessor(message,StompHeaderAccessor.class);if(h==null)return message;
                var command=h.getCommand();var actor=h.getSessionAttributes()==null?null:(SupportChatService.Actor)h.getSessionAttributes().get("supportActor");
                if(command==StompCommand.CONNECT||command==StompCommand.SUBSCRIBE||command==StompCommand.SEND){
                    var httpSession=h.getSessionAttributes()==null?null:(jakarta.servlet.http.HttpSession)h.getSessionAttributes().get("supportHttpSession");
                    validateSession(httpSession,actor);
                }
                if(command==StompCommand.CONNECT){if(actor==null)throw new AccessDeniedException("Chat không hợp lệ.");chat.identity(actor);actors.put(h.getSessionId(),actor);sessions.put(h.getSessionId(),(jakarta.servlet.http.HttpSession)h.getSessionAttributes().get("supportHttpSession"));chat.socketConnected(h.getSessionId(),actor);}
                else if(command==StompCommand.SUBSCRIBE){
                    String target=h.getDestination();if("/topic/support-presence".equals(target)){if(actor==null)throw new AccessDeniedException("Kênh không hợp lệ.");chat.identity(actor);return message;}if(target==null||!target.matches("/topic/support/[0-9]+"))throw new AccessDeniedException("Kênh không được phép.");chat.access(Long.valueOf(target.substring(target.lastIndexOf('/')+1)),actor);
                }else if(command==StompCommand.SEND){
                    String target=h.getDestination();if(target==null||!target.matches("/app/support/[0-9]+/typing"))throw new AccessDeniedException("Không được phát tin trực tiếp lên kênh.");
                    chat.typing(Long.valueOf(target.split("/")[3]),actor);
                    return null;
                }else if(command==StompCommand.DISCONNECT){actors.remove(h.getSessionId());sessions.remove(h.getSessionId());chat.socketDisconnected(h.getSessionId());}
                else if(command!=null&&command!=StompCommand.UNSUBSCRIBE&&command!=StompCommand.ACK&&command!=StompCommand.NACK)throw new AccessDeniedException("Lệnh không được phép.");
                return message;
            }
        });
    }
    @Override public void configureClientOutboundChannel(ChannelRegistration registration){
        registration.interceptors(new ChannelInterceptor(){@Override public Message<?> preSend(Message<?> message,MessageChannel channel){
            var h=StompHeaderAccessor.wrap(message);String destination=h.getDestination();
            if(destination!=null&&destination.startsWith("/topic/support/")){
                try{var actor=actors.get(h.getSessionId());if(actor==null)return null;validateSession(sessions.get(h.getSessionId()),actor);chat.access(Long.valueOf(destination.substring(destination.lastIndexOf('/')+1)),actor);}
                catch(RuntimeException denied){return null;}
            }return message;
        }});
    }
    @org.springframework.context.event.EventListener public void disconnected(org.springframework.web.socket.messaging.SessionDisconnectEvent event){actors.remove(event.getSessionId());sessions.remove(event.getSessionId());chat.socketDisconnected(event.getSessionId());}
    private void validateSession(jakarta.servlet.http.HttpSession session,SupportChatService.Actor actor){
        try{
            if(session==null||actor==null)throw new IllegalStateException();session.getCreationTime();
            if(actor.username()!=null){var context=session.getAttribute("SPRING_SECURITY_CONTEXT");if(!(context instanceof org.springframework.security.core.context.SecurityContext security)||security.getAuthentication()==null||!actor.username().equals(security.getAuthentication().getName()))throw new IllegalStateException();}
        }catch(IllegalStateException ex){throw new AccessDeniedException("Phiên chat đã hết hạn.");}
    }
    @Bean(name="supportChatExecutor") public java.util.concurrent.Executor supportExecutor(){var executor=new ThreadPoolTaskExecutor();executor.setCorePoolSize(2);executor.setMaxPoolSize(4);executor.setQueueCapacity(100);executor.setThreadNamePrefix("support-auto-");executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());executor.initialize();return executor;}
}
