/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/
 */
package org.phenotips.studies.family.internal;

import org.phenotips.data.Patient;
import org.phenotips.studies.family.Family;
import org.phenotips.studies.family.FamilyRepository;

import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.locks.LockModule;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.users.User;
import org.xwiki.users.UserManager;

import java.util.LinkedList;
import java.util.List;
import java.util.Date;

import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link FamilyMembersLockModule}.
 *
 * @version $Id$
 */
public class FamilyMembersLockModuleTest
{
    @Rule
    public final MockitoComponentMockingRule<LockModule> mocker =
        new MockitoComponentMockingRule<LockModule>(FamilyMembersLockModule.class);

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiContext context;

    @Mock
    private XWikiDocument xdoc;

    @Mock
    private XWikiDocument memberDoc;

    @Mock
    private Family family;

    @Mock
    private XWikiLock xlock;

    @Mock
    private Date date;

    @Mock
    private User user;

    @Mock
    private Patient patient1, patient2;

    private DocumentReference doc = new DocumentReference("xwiki", "Family", "F01");

    @Before
    public void setup() throws ComponentLookupException, XWikiException
    {
        MockitoAnnotations.initMocks(this);

        Provider<XWikiContext> contextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(contextProvider.get()).thenReturn(this.context);
        doReturn(this.xwiki).when(this.context).getWiki();
    }

    @Test
    public void familyMembersLockTest() throws ComponentLookupException, XWikiException
    {
        FamilyRepository repo = this.mocker.getInstance(FamilyRepository.class);
        when(this.context.getWiki().getDocument(this.doc, this.context)).thenReturn(this.xdoc);
        when(this.xdoc.getDocumentReference()).thenReturn(this.doc);
        when(repo.getFamilyById("F01")).thenReturn(this.family);

        List<Patient> members = new LinkedList<>();
        members.add(this.patient2);
        members.add(this.patient1);
        when(this.family.getMembers()).thenReturn(members);

        for (Patient member : members) {
            when(member.getXDocument()).thenReturn(this.memberDoc);
            when(this.memberDoc.getLock(this.context)).thenReturn(this.xlock);
            when(this.xlock.getUserName()).thenReturn("Member");
            UserManager userManager = this.mocker.getInstance(UserManager.class);
            when(userManager.getUser(this.xlock.getUserName())).thenReturn(this.user);
            when(this.xlock.getDate()).thenReturn(this.date);
        }

        Assert.assertNotNull(this.mocker.getComponentUnderTest().getLock(this.doc));
    }

    @Test
    public void unlockedTest() throws ComponentLookupException, XWikiException
    {
        FamilyRepository repo = this.mocker.getInstance(FamilyRepository.class);
        when(this.context.getWiki().getDocument(this.doc, this.context)).thenReturn(this.xdoc);
        when(this.xdoc.getDocumentReference()).thenReturn(this.doc);
        when(repo.getFamilyById("F01")).thenReturn(this.family);

        List<Patient> members = new LinkedList<>();
        members.add(this.patient2);
        members.add(this.patient1);
        when(this.family.getMembers()).thenReturn(members);

        for (Patient member : members) {
            when(member.getXDocument()).thenReturn(this.memberDoc);
            when(this.memberDoc.getLock(this.context)).thenReturn(null);
            UserManager userManager = this.mocker.getInstance(UserManager.class);
            when(userManager.getUser(this.xlock.getUserName())).thenReturn(this.user);
            when(this.xlock.getDate()).thenReturn(this.date);
        }

        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.doc));
    }

    @Test
    public void throwsXWikiExceptionTest() throws ComponentLookupException, XWikiException
    {
        when(this.context.getWiki().getDocument(this.doc, this.context))
            .thenThrow(new XWikiException(XWikiException.MODULE_XWIKI_STORE,
                XWikiException.ERROR_XWIKI_STORE_HIBERNATE_READING_DOC,
                "Exception while reading document [xwiki:PhenoTips.FamilyLockModule]"));
        this.mocker.getComponentUnderTest().getLock(this.doc);
        verify(this.mocker.getMockedLogger()).error(anyString(), anyString(),
            Matchers.any(XWikiException.class));
    }

    @Test
    public void emptyFamilyTest() throws ComponentLookupException, XWikiException
    {
        FamilyRepository repo = this.mocker.getInstance(FamilyRepository.class);
        when(this.context.getWiki().getDocument(this.doc, this.context)).thenReturn(this.xdoc);
        when(this.xdoc.getDocumentReference()).thenReturn(this.doc);
        when(repo.getFamilyById("F01")).thenReturn(this.family);
        List<Patient> members = new LinkedList<>();
        when(this.family.getMembers()).thenReturn(members);
        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.doc));
    }

    @Test
    public void noFamilyTest() throws ComponentLookupException, XWikiException
    {
        FamilyRepository repo = this.mocker.getInstance(FamilyRepository.class);
        when(this.context.getWiki().getDocument(this.doc, this.context)).thenReturn(this.xdoc);
        when(this.xdoc.getDocumentReference()).thenReturn(this.doc);
        when(repo.getFamilyById("F001")).thenReturn(null);
        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(this.doc));
    }

    @Test
    public void nullDocumentTest() throws ComponentLookupException, XWikiException
    {
        Assert.assertNull(this.mocker.getComponentUnderTest().getLock(null));
    }

    @Test
    public void priorityIsCorrectTest() throws ComponentLookupException
    {
        Assert.assertEquals(300, this.mocker.getComponentUnderTest().getPriority());
    }
}