package com.example.demodatn.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

public class AccountStatusFilter extends OncePerRequestFilter {
    private final UserDetailsServiceImpl users;
    public AccountStatusFilter(UserDetailsServiceImpl users) { this.users=users; }
    @Override
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)
            throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth!=null && auth.getPrincipal() instanceof CustomUserDetails) {
            boolean enabled;
            CustomUserDetails fresh=null;
            try { fresh=(CustomUserDetails)users.loadUserByUsername(auth.getName()); enabled=fresh.isEnabled(); }
            catch(UsernameNotFoundException ex) { enabled=false; }
            if(!enabled) {
                SecurityContextHolder.clearContext();
                HttpSession session=request.getSession(false);
                if(session!=null)session.invalidate();
                response.sendRedirect(request.getContextPath()+"/login?disabled");
                return;
            }
            String path=request.getRequestURI().substring(request.getContextPath().length());
            boolean allowed=path.equals("/account/change-required") || path.equals("/logout") || path.equals("/error")
                || request.getMethod().equals("GET") && (path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/anh/") || path.startsWith("/images/"));
            if(fresh!=null && fresh.isMustChangePassword() && !allowed) {
                response.setStatus(request.getMethod().equals("GET")?302:303);
                response.setHeader("Location",request.getContextPath()+"/account/change-required");return;
            }
        }
        chain.doFilter(request,response);
    }
}
