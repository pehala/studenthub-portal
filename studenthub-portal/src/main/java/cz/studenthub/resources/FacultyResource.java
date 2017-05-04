/*******************************************************************************
 *     Copyright (C) 2017  Stefan Bunciak
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
package cz.studenthub.resources;

import static cz.studenthub.auth.Consts.ADMIN;
import static cz.studenthub.auth.Consts.AUTHENTICATED;
import static cz.studenthub.auth.Consts.BASIC_AUTH;
import static cz.studenthub.auth.Consts.JWT_AUTH;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import org.pac4j.jax.rs.annotations.Pac4JSecurity;

import cz.studenthub.core.Faculty;
import cz.studenthub.core.User;
import cz.studenthub.core.UserRole;
import cz.studenthub.db.FacultyDAO;
import cz.studenthub.db.UserDAO;
import cz.studenthub.util.PagingUtil;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.IntParam;
import io.dropwizard.jersey.params.LongParam;

@Path("/faculties")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Pac4JSecurity(authorizers = ADMIN, clients = { BASIC_AUTH, JWT_AUTH })
public class FacultyResource {

  private final FacultyDAO facDao;
  private final UserDAO userDao;

  public FacultyResource(FacultyDAO facDao, UserDAO userDao) {
    this.facDao = facDao;
    this.userDao = userDao;
  }

  @GET
  @UnitOfWork
  @Pac4JSecurity(ignore = true)
  public List<Faculty> fetch(@Min(0) @DefaultValue("0") @QueryParam("start") IntParam startParam,
          @Min(0) @DefaultValue("0") @QueryParam("size") IntParam sizeParam) {
      return PagingUtil.paging(facDao.findAll(), startParam.get(), sizeParam.get());
  }

  @GET
  @Path("/{id}")
  @UnitOfWork
  @Pac4JSecurity(ignore = true)
  public Faculty findById(@PathParam("id") LongParam id) {
    return facDao.findById(id.get());
  }

  @DELETE
  @Path("/{id}")
  @UnitOfWork
  public Response delete(@PathParam("id") LongParam idParam) {
    Long id = idParam.get();
    Faculty faculty = facDao.findById(id);
    if (faculty == null)
      throw new WebApplicationException(Status.NOT_FOUND);

    facDao.delete(faculty);
    return Response.noContent().build();
  }

  @PUT
  @Path("/{id}")
  @UnitOfWork
  public Response update(@PathParam("id") LongParam idParam, @NotNull @Valid Faculty faculty) {
    Long id = idParam.get();
    if (facDao.findById(id) == null)
      throw new WebApplicationException(Status.NOT_FOUND);

    faculty.setId(id);
    facDao.update(faculty);
    return Response.ok(faculty).build();
  }

  @POST
  @UnitOfWork
  public Response create(@NotNull @Valid Faculty faculty) {
    facDao.create(faculty);
    if (faculty.getId() == null)
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);

    return Response.created(UriBuilder.fromResource(FacultyResource.class).path("/{id}").build(faculty.getId()))
        .entity(faculty).build();
  }

  @GET
  @Path("/{id}/students")
  @UnitOfWork
  @Pac4JSecurity(authorizers = AUTHENTICATED, clients = { BASIC_AUTH, JWT_AUTH })
  public List<User> fetchStudents(@PathParam("id") LongParam id,
          @Min(0) @DefaultValue("0") @QueryParam("start") IntParam startParam,
          @Min(0) @DefaultValue("0") @QueryParam("size") IntParam sizeParam) {

    Faculty faculty = facDao.findById(id.get());
    if (faculty == null)
      throw new WebApplicationException(Status.NOT_FOUND);

    return PagingUtil.paging(userDao.findByRoleAndFaculty(UserRole.STUDENT, faculty), startParam.get(), sizeParam.get());
  }

  @GET
  @Path("/{id}/supervisors")
  @UnitOfWork
  @Pac4JSecurity(authorizers = AUTHENTICATED, clients = { BASIC_AUTH, JWT_AUTH })
  public List<User> fetchSupervisors(@PathParam("id") LongParam id,
          @Min(0) @DefaultValue("0") @QueryParam("start") IntParam startParam,
          @Min(0) @DefaultValue("0") @QueryParam("size") IntParam sizeParam) {

    Faculty faculty = facDao.findById(id.get());
    if (faculty == null)
      throw new WebApplicationException(Status.NOT_FOUND);

    return PagingUtil.paging(userDao.findByRoleAndFaculty(UserRole.AC_SUPERVISOR, faculty), startParam.get(), sizeParam.get());
  }

}
