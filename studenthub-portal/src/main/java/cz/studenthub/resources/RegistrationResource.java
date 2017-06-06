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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.security.PermitAll;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
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

import cz.studenthub.api.UpdatePasswordBean;
import cz.studenthub.auth.StudentHubPasswordEncoder;
import cz.studenthub.core.Activation;
import cz.studenthub.core.User;
import cz.studenthub.core.UserRole;
import cz.studenthub.db.ActivationDAO;
import cz.studenthub.db.UserDAO;
import cz.studenthub.util.MailClient;
import cz.studenthub.util.SmtpConfig;
import io.dropwizard.auth.Auth;
import io.dropwizard.hibernate.UnitOfWork;
import io.dropwizard.jersey.params.LongParam;

/**
 * User registration and password manipulation endpoints
 * 
 * @author sbunciak
 * @since 1.0
 */
@Path("/account")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RegistrationResource {

  private final MailClient mailer;

  @Inject
  private UserDAO userDao;

  @Inject
  private ActivationDAO actDao;

  public RegistrationResource(SmtpConfig smtpConfig) {
    this.mailer = new MailClient(smtpConfig);
  }

  @POST
  @Path("/signUp")
  @UnitOfWork
  public Response signUp(@NotNull @Valid User user) {

    // check if email is taken
    User sameEmail = userDao.findByEmail(user.getEmail());
    if (sameEmail != null)
      throw new WebApplicationException("Email is already taken.", Status.CONFLICT);

    // check if username is taken
    User sameUsername = userDao.findByUsername(user.getUsername());
    if (sameUsername != null)
      throw new WebApplicationException("Username is already taken.", Status.CONFLICT);

    // check if someone is not trying to reg as ADMIN
    if (user.getRoles().contains(UserRole.ADMIN))
      throw new WebApplicationException("Invalid role - can't register new ADMIN.", Status.BAD_REQUEST);

    // persist user to DB
    userDao.create(user);
    // failed to persist user - server error
    if (user.getId() == null)
      throw new WebApplicationException(Status.INTERNAL_SERVER_ERROR);

    // put user into cache of "inactive" users
    Activation act = new Activation(user);
    actDao.create(act);

    sendActivationEmail(user, act.getActivationCode());

    return Response.created(UriBuilder.fromResource(UserResource.class).path("/{id}").build(user.getId())).entity(user)
        .build();
  }

  @POST
  @Path("/activate")
  @UnitOfWork
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response activate(@QueryParam("secret") String secretKey, @QueryParam("id") LongParam idParam,
      @FormParam("password") String password) {
    User user = userDao.findById(idParam.get());
    Activation act = actDao.findByUser(user);
    if (act != null && act.getActivationCode().equals(secretKey)) {
      // hash and update user password
      user.setPassword(StudentHubPasswordEncoder.encode(password));
      userDao.update(user);
      // send conf. mail
      mailer.sendMessage(user.getEmail(), "User Activation", "activated.html", null);
      // remove user from "inactive" users
      actDao.delete(act);
      return Response.ok().build();
    } else {
      throw new WebApplicationException(Status.FORBIDDEN);
    }
  }

  @POST
  @UnitOfWork
  @Path("/resendActivation")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response resendActivationEmail(@FormParam("email") String email) {
    User user = userDao.findByEmail(email);
    if (user == null)
      throw new WebApplicationException(Status.NOT_FOUND);
    if (user.getPassword() != null)
      throw new WebApplicationException("User is already active.", Status.BAD_REQUEST);

    Activation act = actDao.findByUser(user);
    if (act == null)
      throw new WebApplicationException("No activation to resend.", Status.NOT_FOUND);

    sendActivationEmail(user, act.getActivationCode());

    return Response.ok().build();
  }
  
  @POST
  @UnitOfWork
  @Path("/resetPassword")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response resetPassword(@FormParam("email") String email) {
    User user = userDao.findByEmail(email);
    if (user == null)
      throw new WebApplicationException(Status.NOT_FOUND);

    Activation existing = actDao.findByUser(user);
    if (existing != null) {
      throw new WebApplicationException("Already pending activation.", Status.BAD_REQUEST);
    }

    // "de-activate" user by un-setting his password
    user.setPassword(null);
    userDao.update(user);

    // put user into cache of "inactive" users
    Activation act = new Activation(user);
    actDao.create(act);

    sendActivationEmail(user, act.getActivationCode());

    return Response.ok().build();
  }

  @PUT
  @Path("/{id}/password")
  @UnitOfWork
  @PermitAll
  public Response updatePassword(@PathParam("id") LongParam idParam, UpdatePasswordBean updateBean, @Auth User auth) {
    // only admin or profile owner is allowed
    Long id = idParam.get();
    User user = userDao.findById(id);
    if (user == null)
      throw new WebApplicationException(Status.NOT_FOUND);

    if (id.equals(auth.getId()) || auth.getRoles().contains(UserRole.ADMIN)) {
      // check if old password matches
      if (StudentHubPasswordEncoder.matches(updateBean.getOldPwd(), user.getPassword())) {
        // set new password
        user.setPassword(StudentHubPasswordEncoder.encode(updateBean.getNewPwd()));
        userDao.update(user);
        mailer.sendMessage(user.getEmail(), "Password Updated", "pwdUpdated.html", null);
        return Response.ok().build();
      } else {
        // passwords don't match
        throw new WebApplicationException(Status.BAD_REQUEST);
      }
    } else {
      throw new WebApplicationException(Status.FORBIDDEN);
    }
  }

  private void sendActivationEmail(User user, String secretKey) {
    // send conf. email with activation link
    Map<String, String> args = new HashMap<String, String>();
    args.put("secret", secretKey);
    args.put("name", user.getName());
    args.put("id", user.getId().toString());
    mailer.sendMessage(user.getEmail(), "Password Setup", "setPassword.html", args);
  }
}
