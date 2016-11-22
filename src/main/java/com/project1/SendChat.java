package com.project1;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.owasp.encoder.Encode;

import com.db.Chat;
import com.db.Connector;
/**
 * Servlet implementation class SendChat
 */
public class SendChat extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    
    public SendChat() {
        super();

        
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request,response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String chat = request.getParameter("chat");
		String uid = request.getParameter("userid");
		int user_id = uid!=null?Integer.parseInt(uid):1; //need to be implemented with something else
	
		Session session = null;
        Transaction tx = null;
        SessionFactory sf = null;
        
        try 
        {
        	Connector conn = new Connector();
        	sf = conn.configureSessionFactory();
            session = sf.openSession();
            tx = session.beginTransaction();
             
            String encode=Encode.forHtmlContent(chat);
            encode=Encode.forJavaScript(encode);
            encode=Encode.forXml(encode);
            
            Chat chat1 = new Chat(0,user_id,encode);
            session.save(chat1);
            
            List<Chat> chats = session.createQuery("from Chat").list();
            for(Chat c : chats)
            {
            	System.out.println(c.getUser_id()+ " : "+c.getChat());
            }
             
        } 
        catch (Exception ex) 
        {
            ex.printStackTrace();
            tx.rollback();
        } 
        finally
        {
        	if(session != null && session.isOpen())
			{
        		tx.commit();
				session.flush();
				session.close();
				sf.close();
			}
        	
        	response.sendRedirect("messageBoard.jsp");
        }
	}

}
