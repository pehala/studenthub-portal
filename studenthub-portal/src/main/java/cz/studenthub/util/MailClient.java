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
package cz.studenthub.util;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.Mailer;
import org.simplejavamail.mailer.config.TransportStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple SMTP client leveraging Simple Java Mail lib (http://www.simplejavamail.org)
 * 
 * @author sbunciak
 * @since 1.0
 */
public class MailClient {

  private static Logger LOG = LoggerFactory.getLogger(MailClient.class);

  private final Mailer mailer;
  
  private final SmtpConfig config;

  public MailClient(SmtpConfig config) {
    this.config = config;
    mailer = new Mailer(config.getServer(), config.getPort(), config.getUsername(), config.getPassword(), TransportStrategy.SMTP_TLS);
  }

  public void sendMessage(String recipient, String subject, String templateFile, Map<String, String> args) {
    Email email = new EmailBuilder()
        .from(config.getFromName(), config.getFromEmail())
        .to(recipient)
        .subject(subject)
        .textHTML(loadHtmlFromTemplate(templateFile, args))
        .build();

    // send mail asynchronously
    mailer.sendMail(email, true);
  }

  private String loadHtmlFromTemplate(String templateFile, Map<String, String> args) {
    String htmlContent = "";
    try {
      // load template content
      htmlContent = IOUtils.toString(MailClient.class.getResourceAsStream("/templates/" + templateFile), "UTF-8");
      // replace params
      if (args != null) {
        for (String key : args.keySet()) {
          htmlContent = htmlContent.replaceAll("__" + key + "__", args.get(key));
        }
      }
    } catch (IOException e) {
      LOG.error("Error occured processing email template", e);
    }
    return htmlContent;
  }
}