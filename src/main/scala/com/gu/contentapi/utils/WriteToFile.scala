package com.gu.contentapi.utils

import java.nio.ByteBuffer
import java.io.{ File, FileOutputStream }
import com.amazonaws.services.s3.model.S3Object
import com.amazonaws.util.IOUtils

object WriteToFile {

  def fromLogObject(report: S3Object, path: String): Unit = {
    val ba: Array[Byte] = IOUtils.toByteArray(report.getObjectContent)
    val bb: ByteBuffer = ByteBuffer.wrap(ba)

    val file = new File(path)
    val channel = new FileOutputStream(file).getChannel
    channel.write(bb)
    channel.close()
  }

}

