/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.mock.smtp.server;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import org.apache.james.mock.smtp.server.model.Mail;
import org.subethamail.smtp.DropConnectionException;
import org.subethamail.smtp.RejectException;
import org.subethamail.smtp.server.BaseCommand;
import org.subethamail.smtp.server.Session;
import org.subethamail.smtp.util.EmailUtils;

import com.github.steveash.guavate.Guavate;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;

public class ExtendedMailFromCommand extends BaseCommand {
    public ExtendedMailFromCommand() {
        super("MAIL", "Specifies the sender.", "FROM: <sender> [ <parameters> ]");
    }

    public void execute(String commandString, Session sess) throws IOException, DropConnectionException {
        if (sess.getHasMailFrom()) {
            sess.sendResponse("503 Sender already specified.");
        } else {
            if (commandString.trim().equals("MAIL FROM:")) {
                sess.sendResponse("501 Syntax: MAIL FROM: <address>");
                return;
            }

            String args = this.getArgPredicate(commandString);
            if (!args.toUpperCase(Locale.ENGLISH).startsWith("FROM:")) {
                sess.sendResponse("501 Syntax: MAIL FROM: <address>  Error in parameters: \"" + this.getArgPredicate(commandString) + "\"");
                return;
            }

            String emailAddress = EmailUtils.extractEmailAddress(args, 5);
            if (EmailUtils.isValidEmailAddress(emailAddress)) {
                int size = 0;
                String largs = args.toLowerCase(Locale.ENGLISH);
                int sizec = largs.indexOf(" size=");
                if (sizec > -1) {
                    String ssize = largs.substring(sizec + 6).trim();
                    if (ssize.length() > 0 && ssize.matches("[0-9]+")) {
                        size = Integer.parseInt(ssize);
                    }
                }

                if (size > sess.getServer().getMaxMessageSize()) {
                    sess.sendResponse("552 5.3.4 Message size exceeds fixed limit");
                    return;
                }

                try {
                    sess.startMailTransaction();
                    MockMessageHandler messageHandler = (MockMessageHandler) sess.getMessageHandler();
                    messageHandler.from(emailAddress, parameters(args));
                    sess.setDeclaredMessageSize(size);
                    sess.setHasMailFrom(true);
                    sess.sendResponse("250 Ok");
                } catch (DropConnectionException var9) {
                    throw var9;
                } catch (RejectException var10) {
                    sess.sendResponse(var10.getErrorResponse());
                }
            } else {
                sess.sendResponse("553 <" + emailAddress + "> Invalid email address.");
            }
        }
    }

    private Collection<Mail.Parameter> parameters(String argLine) {
        return Splitter.on(' ').splitToList(argLine)
            .stream()
            .filter(argString -> argString.contains("="))
            .map(this::parameter)
            .collect(Guavate.toImmutableList());
    }

    private Mail.Parameter parameter(String argString) {
        Preconditions.checkArgument(argString.contains("="));
        int index = argString.indexOf('=');

        return Mail.Parameter.builder()
            .name(argString.substring(0, index))
            .value(argString.substring(index + 1))
            .build();
    }
}
