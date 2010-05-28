/**
 * This file Copyright (c) 2005-2009 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */

package com.aptana.syncing.core.tests;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import junit.framework.TestCase;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import com.aptana.ide.core.io.ConnectionContext;
import com.aptana.ide.core.io.CoreIOPlugin;
import com.aptana.ide.core.io.IConnectionPoint;
import com.aptana.syncing.core.model.SyncPair;
import com.aptana.syncing.core.model.SyncPair.Direction;

/**
 * @author Max Stepanov
 *
 */
public abstract class CommonSyncingTest extends TestCase {

	protected static final String TEXT1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse nunc tellus, condimentum quis luctus fermentum, tincidunt eget dui. Sed bibendum iaculis ligula, fringilla ullamcorper justo ullamcorper non. Curabitur tristique mi a magna vestibulum fermentum. Praesent sed neque feugiat purus egestas tristique. Sed non nisi velit. Maecenas placerat, nisi quis iaculis porta, nisi mauris facilisis est, at rutrum lacus sem non ante. Morbi et cursus nibh. Aliquam tincidunt urna quis quam semper ut congue est auctor. Curabitur malesuada, diam ut congue elementum, orci eros rhoncus felis, vel elementum felis velit id eros. Quisque eros diam, malesuada nec tincidunt eget, gravida iaculis tortor. Donec sollicitudin ultricies ante ac facilisis. In egestas malesuada erat id vehicula.\n" //$NON-NLS-1$
			+ "Integer non urna nunc, et rhoncus eros. Suspendisse tincidunt laoreet enim vel pretium. Nam bibendum sodales risus nec adipiscing. Pellentesque fringilla interdum odio posuere consectetur. Nullam venenatis augue sed felis tempus eu posuere quam facilisis. Pellentesque commodo rutrum bibendum. Ut sit amet sapien in purus vestibulum sodales. Integer pharetra mi in dui auctor in tristique erat malesuada. Integer nec ipsum quam. Quisque non enim et quam consequat mollis id ac sem. Nunc ut elit ac odio adipiscing pretium vel eget mauris. Aenean diam diam, porttitor sit amet lobortis a, accumsan at ante. Phasellus ut nulla enim. In nec diam magna. In molestie vulputate viverra. Etiam at justo tellus, sed rutrum erat.\r\n" //$NON-NLS-1$
			+ "Duis consectetur ornare ante, sit amet ultricies leo aliquam vitae. In fermentum nisi non dolor viverra non hendrerit nulla malesuada. Mauris adipiscing aliquet fringilla. Curabitur porttitor tristique massa, et semper nulla semper et. Phasellus a ipsum eu lectus pulvinar aliquam eget viverra velit. Sed commodo ultrices pulvinar. In at felis sollicitudin lorem semper scelerisque. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Proin vel purus id odio malesuada gravida. Pellentesque habitant morbi tristique senectus et netus et malesuada fames ac turpis egestas. Quisque metus mi, eleifend consectetur varius vitae, euismod eget nulla. Morbi justo felis, accumsan vel tempor non, rutrum at augue. Curabitur nulla lorem, ultricies a lobortis in, semper vitae diam. Pellentesque nec orci non turpis dignissim mollis. Quisque quis sapien vitae ligula iaculis dapibus sed at quam. Nullam ut nisl id eros sagittis rutrum a vitae risus. Suspendisse lacinia lacinia rutrum. Fusce molestie pellentesque dapibus. Quisque eu orci dolor, eget venenatis velit.\n" //$NON-NLS-1$
			+ "Nam rhoncus gravida ultrices. Maecenas hendrerit diam pharetra mauris commodo eleifend. Etiam ullamcorper aliquet arcu, sit amet luctus risus scelerisque at. Praesent nibh eros, rutrum in imperdiet eget, dignissim ornare nisl. Fusce sollicitudin, turpis id volutpat tincidunt, diam nibh euismod eros, eget tempor justo nulla ut magna. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Vivamus eu neque ac ante varius imperdiet. Vestibulum blandit neque lacus, a suscipit mi. Maecenas aliquet, lorem ut interdum bibendum, velit tellus feugiat quam, non posuere leo justo eget ante. Aliquam mattis augue est, et malesuada libero. Suspendisse nisl tellus, tempus sit amet luctus quis, vulputate eu turpis. Morbi lobortis vulputate odio at faucibus. Cras ut nisi ipsum."; //$NON-NLS-1$
	
	protected static final String TEXT2 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Morbi accumsan, arcu in congue hendrerit, sem nulla ullamcorper odio, sed aliquam lacus nibh nec sem. Sed venenatis, ante sed porta sagittis, tellus neque adipiscing augue, vitae gravida nisl leo ac quam. Sed congue sem eros, sed lacinia eros. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras sed erat erat. Fusce non dui dui, nec aliquet est. Aliquam quis justo et eros consequat mollis fermentum at dui. Etiam ut augue tellus. Phasellus eget dolor ipsum, a viverra metus. Mauris accumsan consectetur posuere. Ut molestie convallis urna, vel cursus felis laoreet pellentesque. Mauris non ante sed est pharetra ultrices quis nec lorem. Nullam commodo nunc ac nulla aliquet et venenatis quam dapibus. Pellentesque viverra, enim in fringilla gravida, ante risus venenatis leo, nec rutrum ligula urna at risus. Curabitur eu purus sit amet ipsum hendrerit facilisis. Nulla et gravida nisi.\n" //$NON-NLS-1$
			+ "Aliquam felis ipsum, pretium ac euismod eget, lacinia non lectus. Duis quis tortor nec sem scelerisque semper. Curabitur metus augue, pharetra eu eleifend sed, porttitor at magna. Integer mi velit, ornare sit amet elementum eu, scelerisque posuere risus. Donec sed ante eros, eu consectetur nulla. Aenean mollis commodo purus, blandit tristique est luctus in. Nam tristique turpis a lectus convallis euismod. Aliquam consectetur, nisi ac mollis gravida, eros diam faucibus metus, feugiat viverra risus lectus eget nunc. Sed auctor aliquet vulputate. Aenean quis leo in dui scelerisque hendrerit at non neque. Aenean ornare tristique mollis. Morbi vitae velit ac orci tincidunt porttitor.\n" //$NON-NLS-1$
			+ "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris pellentesque ipsum id lorem accumsan dapibus. Maecenas ac sapien orci, eget ornare sem. Duis semper, magna sed ullamcorper interdum, nibh enim sodales mauris, at commodo risus turpis imperdiet diam. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Phasellus semper tortor at diam condimentum sodales. Cras adipiscing interdum sollicitudin. Sed vel nulla ac magna convallis iaculis. Vivamus aliquet aliquam libero sit amet rutrum. Vestibulum in fermentum purus. Proin auctor elit turpis, ut feugiat velit. Nullam tristique fermentum urna, non luctus mi scelerisque eget."; //$NON-NLS-1$

	protected static final String TEXT3 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Proin elit velit, viverra quis convallis vel, pellentesque a felis. Cras diam risus, lacinia vitae varius et, vulputate a magna. Sed facilisis est vel neque interdum convallis. Cras aliquet sem non velit egestas molestie vitae quis odio. Etiam aliquam mollis libero. Sed at tortor orci, ac suscipit ipsum. Duis aliquet urna non tortor euismod dignissim. Donec vitae orci sit amet turpis porta congue sed ultricies neque. Donec sollicitudin aliquam malesuada. Nunc rutrum sollicitudin dui, nec porttitor leo lacinia eu. Nulla facilisi. Suspendisse porttitor enim non lacus mattis ut luctus metus pulvinar. Maecenas rutrum porttitor risus non tempus. Etiam eget ultricies mauris. Aliquam et metus mi. Nullam ornare ultrices odio, in sagittis diam dignissim eu. Praesent pellentesque consectetur condimentum. Curabitur imperdiet suscipit posuere. Nam viverra suscipit dui quis convallis. Nulla orci justo, feugiat sed rutrum luctus, rutrum et tellus." //$NON-NLS-1$
			+ "Proin metus tellus, rutrum quis consectetur sed, dictum congue magna. Vestibulum vitae nisl sit amet ante iaculis fringilla ut in libero. Morbi auctor aliquam accumsan. Nam rutrum augue ut est sodales vitae molestie elit consequat. In nec porttitor nisi. Cras a turpis turpis, a placerat purus. Mauris ac nisi nec velit blandit convallis. Nullam feugiat hendrerit iaculis. Donec eget nunc nisi, at facilisis mauris. Mauris sollicitudin, lectus et volutpat aliquam, augue metus lobortis nibh, a egestas diam quam mattis quam." //$NON-NLS-1$
			+ "In consectetur metus nec nulla pellentesque quis fermentum sem venenatis. Sed libero sapien, tempor cursus iaculis sed, pellentesque a sapien. Nullam a libero at leo vestibulum vehicula vitae non nunc. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Morbi rhoncus turpis vitae nunc viverra hendrerit elementum turpis malesuada. Aenean malesuada aliquet risus in porttitor. Morbi rutrum, lacus non viverra sagittis, libero nisl pharetra quam, sed aliquam ipsum felis non elit. Pellentesque posuere interdum posuere. Curabitur placerat posuere eros ac pharetra. Integer rhoncus tempus mi, eget sagittis mi dictum ut. Mauris sit amet neque in dui mollis fermentum."; //$NON-NLS-1$
	
	protected static final String TEXT4 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Mauris ullamcorper nibh volutpat turpis euismod lobortis. Quisque mauris nisi, blandit a molestie vel, posuere et orci. Curabitur convallis accumsan elit, et posuere orci pretium sed. Duis nisi felis, feugiat pellentesque viverra sed, tincidunt in risus. Suspendisse potenti. Praesent nisl augue, faucibus vel pellentesque ut, ornare nec orci. Maecenas malesuada orci at massa porttitor blandit. Duis fringilla nisi vel sem eleifend eu sodales ante sodales. Maecenas vel ullamcorper ipsum. In commodo fermentum arcu eget sodales. Nunc urna magna, semper nec tempus sit amet, facilisis id dui. Quisque quis ultrices diam. Aliquam scelerisque fermentum commodo. Quisque venenatis semper neque, nec bibendum tortor mollis nec. Nullam aliquet lobortis elit, non pharetra nisl fermentum eget. Nam sollicitudin elit id lectus dignissim a vehicula velit volutpat." //$NON-NLS-1$
			+ "Donec et mi in libero tempus placerat. Praesent bibendum tellus eget quam cursus quis scelerisque purus egestas. Praesent quis orci et lacus vulputate molestie id vitae mauris. Pellentesque eros massa, mattis id facilisis non, ultricies vel diam. Etiam iaculis auctor sapien, ac aliquet lacus vehicula eget. Proin id ante lacus. Sed rhoncus ipsum sit amet metus tincidunt a rutrum mauris laoreet. Suspendisse potenti. Nulla sed quam nec dolor aliquet congue nec faucibus ipsum. Duis ullamcorper rutrum lacinia. Fusce placerat volutpat augue in tincidunt. Vivamus convallis tortor non odio semper ornare. Aliquam sagittis placerat nibh, ac ultrices magna laoreet ut." //$NON-NLS-1$
			+ "Vestibulum metus arcu, facilisis vitae laoreet feugiat, mattis ac sapien. Phasellus non elit neque, porta facilisis nulla. Donec egestas euismod dapibus. Quisque ut urna id neque aliquam molestie. Phasellus eu diam odio. Suspendisse nibh eros, pretium sed ultrices eget, vulputate non erat. Cras vehicula tortor quis libero viverra eget sagittis tortor dapibus. Donec vulputate, tortor a ullamcorper vulputate, est eros aliquam dui, a lacinia ligula sem a enim. Integer iaculis molestie feugiat. Aenean ut magna urna. Suspendisse pulvinar, lorem non pulvinar convallis, urna orci elementum velit, ac scelerisque magna neque at est. Donec vulputate adipiscing ipsum, sed porta turpis rutrum id. Vestibulum tincidunt, purus et ultrices pulvinar, risus velit tristique tortor, at blandit velit quam ut massa. Donec vehicula lobortis porttitor. Sed mi felis, cursus id ultrices quis, rhoncus convallis est."; //$NON-NLS-1$
	
	protected IConnectionPoint leftCP;
	protected IConnectionPoint rightCP;
	
	protected IFileStore leftFileStore;
	protected IFileStore rightFileStore;
		
	private static int counter = 0;
	
	@Override
	protected void setUp() throws Exception {
		ConnectionContext context = new ConnectionContext();
		context.put(ConnectionContext.COMMAND_LOG, System.out);
		CoreIOPlugin.setConnectionContext(leftCP, context);
		CoreIOPlugin.setConnectionContext(rightCP, context);

		leftFileStore = setUpSide(leftCP);
		rightFileStore = setUpSide(rightCP);
	}

	@Override
	protected void tearDown() throws Exception {
		tearDownSide(leftCP, leftFileStore);
		tearDownSide(rightCP, rightFileStore);
	}

	private IFileStore setUpSide(IConnectionPoint cp) throws CoreException {
		IFileStore fs = cp.getRoot().getFileStore(Path.ROOT.append(getClass().getSimpleName() + System.currentTimeMillis()).addFileExtension(Integer.toHexString(counter++)));
		assertNotNull(fs);
		return fs;
	}

	private void tearDownSide(IConnectionPoint cp, IFileStore fs) throws CoreException {
		try {
			if (fs.fetchInfo().exists()) {
				fs.delete(EFS.NONE, null);
				assertFalse(fs.fetchInfo().exists());
			}
		} finally {
			if (cp.isConnected()) {
				cp.disconnect(null);
			}
		}		
	}
	
	private SyncPair createSyncPair() {
		assertNotNull(leftFileStore);
		assertNotNull(rightFileStore);
		return new SyncPair(leftFileStore, rightFileStore);
	}
	
	private Direction calculateDirection() throws CoreException {
		return createSyncPair().calculateDirection(new NullProgressMonitor());
	}
	
	private boolean synchronize() throws CoreException {
		return createSyncPair().synchronize(new NullProgressMonitor());
	}
	
	private void assertDirection(Direction direction) throws CoreException {
		assertEquals(direction, calculateDirection());
	}
	
	private static void createFolder(IFileStore fs) throws CoreException {
		fs.mkdir(EFS.NONE, new NullProgressMonitor());
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertTrue(fi.isDirectory());
		assertFalse(fi.getAttribute(EFS.ATTRIBUTE_SYMLINK));
	}
	
	private static void writeFile(IFileStore fs, String content) throws CoreException, IOException {
		Writer w = new OutputStreamWriter(fs.openOutputStream(EFS.NONE, null));
		w.write(content);
		w.close();
		IFileInfo fi = fs.fetchInfo();
		assertNotNull(fi);
		assertTrue(fi.exists());
		assertFalse(fi.isDirectory());
		assertFalse(fi.getAttribute(EFS.ATTRIBUTE_SYMLINK));
		assertEquals(content.length(), fi.getLength());
	}
	
	private static void copyFile(IFileStore source, IFileStore destination) throws CoreException {
		source.copy(destination, EFS.SHALLOW, new NullProgressMonitor());		
	}
	
	private static void delete(IFileStore fs) throws CoreException {
		fs.delete(EFS.NONE, new NullProgressMonitor());
	}
	
	
	public final void testNonexistingBoth() throws CoreException {
		assertDirection(Direction.SAME);
		assertTrue(synchronize());
		assertDirection(Direction.SAME); // verify that state is consistent
	}

	public final void testNewFolderLeft() throws CoreException {
		createFolder(leftFileStore);
		assertDirection(Direction.LEFT_TO_RIGHT);
		assertTrue(synchronize());		
		assertDirection(Direction.SAME); // verify that state is consistent
	}

	public final void testNewFolderRight() throws CoreException {
		createFolder(rightFileStore);
		assertDirection(Direction.RIGHT_TO_LEFT);
		assertTrue(synchronize());		
		assertDirection(Direction.SAME); // verify that state is consistent
	}

	public final void testNewFolderBoth() throws CoreException {
		createFolder(leftFileStore);
		createFolder(rightFileStore);
		assertDirection(Direction.SAME);
		assertTrue(synchronize());		
		assertDirection(Direction.SAME); // verify that state is consistent
	}

	public final void testNewFileLeft() throws CoreException, IOException {
		writeFile(leftFileStore, TEXT1);
		assertDirection(Direction.LEFT_TO_RIGHT);
		assertTrue(synchronize());		
		assertDirection(Direction.SAME); // verify that state is consistent
	}

	public final void testNewFileRight() throws CoreException, IOException {
		writeFile(rightFileStore, TEXT1);
		assertDirection(Direction.RIGHT_TO_LEFT);
		assertTrue(synchronize());		
		assertDirection(Direction.SAME); // verify that state is consistent
	}

	public final void testNewFolderLeftNewFileRight() throws CoreException, IOException {
		createFolder(leftFileStore);
		writeFile(rightFileStore, TEXT1);
		assertDirection(Direction.INCONSISTENT);
		assertFalse(synchronize());		
		assertDirection(Direction.INCONSISTENT); // verify that state is consistent
	}

	public final void testNewFolderRightNewFileLeft() throws CoreException, IOException {
		writeFile(leftFileStore, TEXT1);
		createFolder(rightFileStore);
		assertDirection(Direction.INCONSISTENT);
		assertFalse(synchronize());		
		assertDirection(Direction.INCONSISTENT); // verify that state is consistent
	}

	public final void testNewFileBothDifferent() throws CoreException, IOException {
		writeFile(leftFileStore, TEXT1);
		writeFile(rightFileStore, TEXT2);
		assertDirection(Direction.AMBIGUOUS);
		assertFalse(synchronize());		
		assertDirection(Direction.AMBIGUOUS); // verify that state is consistent
	}

	public final void testNewFileBothSame() throws CoreException, IOException {
		writeFile(leftFileStore, TEXT1);
		copyFile(leftFileStore, rightFileStore);
		assertDirection(Direction.SAME);
		assertTrue(synchronize());		
		assertDirection(Direction.SAME); // verify that state is consistent
	}

	public final void testDeletedFolderLeft() throws CoreException {
		createFolder(leftFileStore);
		createFolder(rightFileStore);
		assertDirection(Direction.SAME);
		assertTrue(synchronize());
		assertDirection(Direction.SAME);
		// end of initial state
		delete(leftFileStore);
		assertDirection(Direction.LEFT_TO_RIGHT);
		assertTrue(synchronize());
		assertDirection(Direction.SAME); // verify that state is consistent
	}

	public final void testDeletedFolderRight() throws CoreException {
		createFolder(leftFileStore);
		createFolder(rightFileStore);
		assertDirection(Direction.SAME);
		assertTrue(synchronize());
		assertDirection(Direction.SAME);
		// end of initial state
		delete(rightFileStore);
		assertDirection(Direction.RIGHT_TO_LEFT);
		assertTrue(synchronize());
		assertDirection(Direction.SAME); // verify that state is consistent
	}

}
