/***************************************************************************
 * Copyright (C) 2009 Atlas of Living Australia
 * All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public
 * License Version 1.1 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 ***************************************************************************/


package org.ala.dao.hibernate;

import java.util.List;

import org.ala.dao.WebServiceDAO;
import org.ala.model.WebService;
import org.hibernate.Query;
import org.hibernate.Session;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * 
 *
 * @author Dave Martin (David.Martin@csiro.au)
 */
public class WebServiceDAOImpl extends HibernateDaoSupport implements
		WebServiceDAO {

	/**
	 * @see org.ala.dao.WebServiceDAO#create(org.ala.model.WebService)
	 */
	public void create(WebService webService) {
	}

	/**
	 * @see org.ala.dao.WebServiceDAO#getAll()
	 */
	@SuppressWarnings("unchecked")
	public List<WebService> getAll() {
		return (List<WebService>) getHibernateTemplate().execute(new HibernateCallback() {
			public Object doInHibernate(Session session) {
				Query query = session.createQuery("from WebService order by isoCountryCode");
				return query.list();
			}
		});
	}

	/**
	 * @see org.ala.dao.WebServiceDAO#update(org.ala.model.WebService)
	 */
	public void update(WebService webService) {
	}
}
