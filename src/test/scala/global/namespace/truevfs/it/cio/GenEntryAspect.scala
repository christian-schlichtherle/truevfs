package global.namespace.truevfs.it.cio

import global.namespace.truevfs.commons.cio.Entry

/**
 * @author Christian Schlichtherle
 */
trait GenEntryAspect[E <: Entry] {

  def entry: E

  final def name = entry.getName
}
