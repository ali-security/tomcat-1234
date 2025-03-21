/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.catalina.tribes.group.interceptors;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.catalina.tribes.ChannelMessage;
import org.apache.catalina.tribes.Member;
import org.apache.catalina.tribes.group.ChannelInterceptorBase;
import org.apache.catalina.tribes.membership.Membership;
import org.apache.catalina.tribes.util.StringManager;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

/**
 * Filters membership based on domain.
 */
public class DomainFilterInterceptor extends ChannelInterceptorBase implements DomainFilterInterceptorMBean {

    private static final Log log = LogFactory.getLog(DomainFilterInterceptor.class);
    protected static final StringManager sm = StringManager.getManager(DomainFilterInterceptor.class);
    protected volatile Membership membership = null;

    protected byte[] domain = new byte[0];
    protected int logInterval = 100;
    private final AtomicInteger logCounter = new AtomicInteger(logInterval);

    @Override
    public void messageReceived(ChannelMessage msg) {
        if (Arrays.equals(domain, msg.getAddress().getDomain())) {
            super.messageReceived(msg);
        } else {
            if (logCounter.incrementAndGet() >= logInterval) {
                logCounter.set(0);
                if (log.isWarnEnabled()) {
                    log.warn(sm.getString("domainFilterInterceptor.message.refused", msg.getAddress()));
                }
            }
        }
    }// messageReceived


    @Override
    public void memberAdded(Member member) {
        if (membership == null) {
            setupMembership();
        }
        boolean notify;
        synchronized (membership) {
            notify = Arrays.equals(domain, member.getDomain());
            if (notify) {
                notify = membership.memberAlive(member);
            }
        }
        if (notify) {
            super.memberAdded(member);
        } else {
            if (log.isInfoEnabled()) {
                log.info(sm.getString("domainFilterInterceptor.member.refused", member));
            }
        }
    }

    @Override
    public void memberDisappeared(Member member) {
        if (membership == null) {
            setupMembership();
        }
        boolean notify;
        synchronized (membership) {
            notify = Arrays.equals(domain, member.getDomain());
            if (notify) {
                membership.removeMember(member);
            }
        }
        if (notify) {
            super.memberDisappeared(member);
        }
    }

    @Override
    public boolean hasMembers() {
        if (membership == null) {
            setupMembership();
        }
        return membership.hasMembers();
    }

    @Override
    public Member[] getMembers() {
        if (membership == null) {
            setupMembership();
        }
        return membership.getMembers();
    }

    @Override
    public Member getMember(Member mbr) {
        if (membership == null) {
            setupMembership();
        }
        return membership.getMember(mbr);
    }

    @Override
    public Member getLocalMember(boolean incAlive) {
        return super.getLocalMember(incAlive);
    }


    protected synchronized void setupMembership() {
        if (membership == null) {
            membership = new Membership(super.getLocalMember(true));
        }

    }

    @Override
    public byte[] getDomain() {
        return domain;
    }

    public void setDomain(byte[] domain) {
        this.domain = domain;
    }

    public void setDomain(String domain) {
        if (domain == null) {
            return;
        }
        if (domain.startsWith("{")) {
            setDomain(org.apache.catalina.tribes.util.Arrays.fromString(domain));
        } else {
            setDomain(org.apache.catalina.tribes.util.Arrays.convert(domain));
        }
    }

    @Override
    public int getLogInterval() {
        return logInterval;
    }

    @Override
    public void setLogInterval(int logInterval) {
        this.logInterval = logInterval;
    }

}
