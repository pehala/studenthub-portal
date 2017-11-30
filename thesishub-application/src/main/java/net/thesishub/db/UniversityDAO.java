/*******************************************************************************
 *     Copyright (C) 2016  Stefan Bunciak
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package net.thesishub.db;

import java.util.List;

import org.hibernate.SessionFactory;

import io.dropwizard.hibernate.AbstractDAO;
import net.thesishub.core.University;

/**
 * Data(base) Access Object for University objects.
 * 
 * @author sbunciak
 * @since 1.0
 */
public class UniversityDAO extends AbstractDAO<University> {

  public UniversityDAO(SessionFactory sessionFactory) {
    super(sessionFactory);
  }

  public University update(University university) {
    currentSession().clear();
    return persist(university);
  }

  public University create(University university) {
    return persist(university);
  }

  public University findById(Long id) {
    return get(id);
  }

  @SuppressWarnings("unchecked")
  public List<University> findAll() {
    return list(namedQuery("University.findAll"));
  }

  public void delete(University university) {
    currentSession().delete(university);
  }

  public List<University> search(String text) {
    return list(query("SELECT u FROM University u WHERE lower(u.name) LIKE :pattern OR lower(u.city) LIKE :pattern")
        .setParameter("pattern", "%" + text.toLowerCase() + "%"));
  }
}
