package com.liam.config;

import java.sql.Driver;

import javax.sql.DataSource;

import org.activiti.engine.FormService;
import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ActivitiConfig {

	@Value ("${activiti.datasource.url}")
	private String url;
	
    @Value("${activiti.datasource.username}")
    private String username;
    
    @Value("${activiti.datasource.password}")
    private String password;
    
    @Value("${activiti.datasource.driver-class-name}")
    private String driverClassName;

	/**
	 * 配置Activiti Data Source
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings("unchecked")
	@Bean
	public DataSource activitiDataSource() throws ClassNotFoundException {
		SimpleDriverDataSource dataSource = new SimpleDriverDataSource();
		dataSource.setUrl(url);
		dataSource.setDriverClass((Class<? extends Driver>) Class.forName(driverClassName));
		dataSource.setUsername(username);
		dataSource.setPassword(password);
		return dataSource;
	}

	/**
	 * 配置Transaction Manager
	 * @throws ClassNotFoundException 
	 */
	@Bean
	public PlatformTransactionManager transactionManager() throws ClassNotFoundException {
		return new DataSourceTransactionManager(activitiDataSource());
	}

	/**
	 * 配置並產生流程引擎設置實例
	 * @throws ClassNotFoundException 
	 */
	@Bean
	public ProcessEngineConfigurationImpl processEngineConfiguration() throws ClassNotFoundException {
		SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
		processEngineConfiguration.setDataSource(activitiDataSource());
		processEngineConfiguration.setDatabaseSchemaUpdate("true");
		processEngineConfiguration.setTransactionManager(transactionManager());
		processEngineConfiguration.setJobExecutorActivate(false); // Activiti 5
		// processEngineConfiguration.setAsyncExecutorActivate(true); //Activiti 6
		processEngineConfiguration.setAsyncExecutorEnabled(true); // Activiti 5
		processEngineConfiguration.setAsyncExecutorActivate(true);
		processEngineConfiguration.setHistory("full");

		return processEngineConfiguration;
	}

	/**
	 * 利用流程引擎設置實例產生流程引擎FactoryBean
	 * @throws ClassNotFoundException 
	 */
	@Bean
	public ProcessEngineFactoryBean processEngineFactoryBean() throws ClassNotFoundException {
		ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();
		processEngineFactoryBean.setProcessEngineConfiguration(processEngineConfiguration());
		return processEngineFactoryBean;
	}

	/**
	 * 取得ProcessEngine
	 */
	@Bean
	public ProcessEngine processEngine() {

		try {
			return processEngineFactoryBean().getObject();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@Bean
	public RepositoryService repositoryService() {
		return processEngine().getRepositoryService();
	}

	@Bean
	public RuntimeService runtimeService() {
		return processEngine().getRuntimeService();
	}

	@Bean
	public TaskService taskService() {
		return processEngine().getTaskService();
	}

	@Bean
	public HistoryService historyService() {
		return processEngine().getHistoryService();
	}

	@Bean
	public FormService formService() {
		return processEngine().getFormService();
	}

	@Bean
	public IdentityService identityService() {
		return processEngine().getIdentityService();
	}

	@Bean
	public ManagementService managementService() {
		return processEngine().getManagementService();
	}

}
