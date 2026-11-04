package com.example.demodatn.security;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/** One policy for URL authorization, menu entries and individual commands. */
@Component
public class AdminAccess {
    public boolean hasRole(Authentication auth,String role){return auth!=null&&auth.isAuthenticated()&&auth.getAuthorities().stream().anyMatch(a->role.equals(a.getAuthority()));}
    public boolean isAdmin(Authentication auth){return hasRole(auth,"ROLE_ADMIN");}
    private boolean under(String path,String root){return path.equals(root)||path.startsWith(root+"/");}
    public boolean allowed(Authentication auth,String method,String path){
        if(isAdmin(auth))return true;
        if(!hasRole(auth,"ROLE_EMPLOYEE"))return false;
        if("POST".equals(method)&&path.equals("/admin/accounts/username-suggestion"))return true;
        if(("GET".equals(method)||"POST".equals(method))&&under(path,com.example.demodatn.controller.admin.donhang.AdminOrderController.LEGACY_PATH))return true;
        if(under(path,"/admin/pos")||under(path,"/admin/ban-online")||under(path,"/admin/invoices")||under(path,"/admin/account")||under(path,"/admin/support-tickets")||under(path,"/admin/chatbot"))return true;
        if("GET".equals(method)&&path.equals("/admin/inventory"))return true;
        if(under(path,"/admin/customers")){
            if("GET".equals(method))return true;
            return "POST".equals(method)&&(path.equals("/admin/customers/save")||path.matches("/admin/customers/[0-9]+/addresses(?:/.*)?")||path.matches("/admin/customers/[0-9]+/send-account"));
        }
        return false;
    }
    public boolean customerAccount(Authentication auth,com.example.demodatn.entity.nhanvien.User user){
        return isAdmin(auth)||hasRole(auth,"ROLE_EMPLOYEE")&&(user==null||user.getRole()!=null&&"ROLE_USER".equals(user.getRole().getName()));
    }
    public String home(Authentication auth){return isAdmin(auth)?"/admin":hasRole(auth,"ROLE_EMPLOYEE")?"/admin/pos":"/";}
}
