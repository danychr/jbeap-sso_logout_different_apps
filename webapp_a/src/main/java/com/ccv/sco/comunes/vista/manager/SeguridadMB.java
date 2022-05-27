package com.ccv.sco.comunes.vista.manager;

import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

//import org.richfaces.component.UICommandButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Named
@SessionScoped
public class SeguridadMB implements Serializable {

	private static final long serialVersionUID = 918159846826645500L;
	private static final Logger LOG = LoggerFactory.getLogger(SeguridadMB.class);
	private String username;
	private String password;
	private static final String INDEX = "index";

	public SeguridadMB() {
		super();
	}

	@PostConstruct
	public void init() {
	    LOG.info("Inicializando seguridadMB...");
	}


	private void realLogin() {
		FacesContext context = FacesContext.getCurrentInstance();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
			try {
				request.login(getUsername(), getPassword());
			} catch (Exception e) {
				LOG.error("Error en login", e);
			}
	}

	public String login() {
				realLogin();
		LOG.info("login() correcto");
		return INDEX;
	}

	public String logout() {
		LOG.debug("logout sco..");
		FacesContext context = FacesContext.getCurrentInstance();
		context.getExternalContext().invalidateSession();
		HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
		try {
			request.getSession().invalidate();
			request.logout();
		} catch (ServletException e) {
			LOG.error("Ocurrio un error al hacer logout del sistema.", e);
		}
		return "/index.xhtml?faces-redirect=true";
	}

	/**
	 * @return the username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(String password) {
		this.password = password;
	}

   
	public String getSessionId() {
		String sessionId = "";
		Object session = FacesContext.getCurrentInstance().getExternalContext().getSession(false);
		if (session != null && session instanceof HttpSession) {
			sessionId = ((HttpSession) session).getId();
		}
		return sessionId;
	}
}
