package com.aimir.fep.iot.service.dao;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import com.aimir.fep.iot.domain.common.RSCException;
import com.aimir.fep.iot.domain.resources.ContainerOnlyId;
import com.aimir.fep.iot.domain.resources.MgmtCmdOnlyId;
import com.mongodb.DBObject;

@Repository 
public class GenericMongoDao {
	protected String collectionName = null;
	protected Class cls = null;
	
	@Autowired
	protected MongoTemplate mongoTemplate;
	
	/**
	 * upsert
	 * @param obj
	 * @throws Exception
	 */
	public void upsert(Object obj) throws Exception {
		mongoTemplate.save(obj, this.collectionName);
	}

	/**
	 * insert
	 * @param obj
	 * @throws Exception
	 */
	public void insert(Object obj) throws Exception {
		mongoTemplate.insert(obj, this.collectionName);
	}

	/**
	 * find
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public List<?> find(Query query) throws Exception {
		return  mongoTemplate.find(query, this.cls, this.collectionName);
	}
	
	/**
	 * findOne
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public Object findOne(Query query) throws Exception {
		return mongoTemplate.findOne(query, this.cls, this.collectionName);
	}

	/**
	 * findAll
	 * @return
	 * @throws Exception
	 */
	public List<?> findAll() throws Exception {
		return  mongoTemplate.findAll(this.cls, this.collectionName);
	}

	/**
	 * count
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public long count(Query query) throws Exception { 
		return mongoTemplate.count(query, this.collectionName);
	}

	/**
	 * update
	 * @param query
	 * @param update
	 * @throws Exception
	 */
	public void update(Query query, Update update) throws Exception {
		mongoTemplate.upsert(query, update, collectionName);
	}

	/**
	 * remove
	 * @param query
	 * @throws Exception
	 */
	public void remove(Query query) throws Exception {
		mongoTemplate.remove(query, this.collectionName);
	}

	/**
	 * remove
	 * @param obj
	 * @throws Exception
	 */
	public void remove(Object obj) throws Exception {
		mongoTemplate.remove(obj, this.collectionName);
	}
	
	/**
	 * findAndModify
	 * @param query
	 * @param update
	 * @throws Exception
	 */
	public void findAndModify(Query query, Update update) throws Exception {
		mongoTemplate.findAndModify(query, update, this.cls, this.collectionName);
	}
	
	/**
	 * findContainerOnlyID
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public List<?> findContainerOnlyID(Query query) throws Exception {
		return  mongoTemplate.find(query, ContainerOnlyId.class, this.collectionName);
	}

	/**
	 * findMgmtCmdOnlyID
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public List<?> findMgmtCmdOnlyID(Query query) throws Exception {
		return  mongoTemplate.find(query, MgmtCmdOnlyId.class, this.collectionName);
	}

	/**
	 * getConverter
	 * @param clazz
	 * @param dbObject
	 * @return
	 * @throws Exception
	 */
	public Object getConverter(Class<?> clazz, DBObject dbObject) throws Exception {
		return mongoTemplate.getConverter().read(clazz, dbObject);
	}
}
