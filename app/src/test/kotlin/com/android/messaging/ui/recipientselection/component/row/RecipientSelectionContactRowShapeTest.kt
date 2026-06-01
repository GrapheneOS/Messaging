package com.android.messaging.ui.recipientselection.component.row

import androidx.compose.foundation.shape.RoundedCornerShape
import com.android.messaging.ui.recipientselection.component.contactCornerRadius
import com.android.messaging.ui.recipientselection.component.contactMiddleCornerRadius
import com.android.messaging.ui.recipientselection.component.recipientSelectionContactRowShape
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
internal class RecipientSelectionContactRowShapeTest {

    @Test
    fun recipientSelectionContactRowShape_mapsListPositionToGroupedShape() {
        assertEquals(
            RoundedCornerShape(size = contactCornerRadius),
            recipientSelectionContactRowShape(index = 0, totalCount = 0),
        )
        assertEquals(
            RoundedCornerShape(size = contactCornerRadius),
            recipientSelectionContactRowShape(index = 0, totalCount = 1),
        )
        assertEquals(
            RoundedCornerShape(
                topStart = contactCornerRadius,
                topEnd = contactCornerRadius,
                bottomStart = contactMiddleCornerRadius,
                bottomEnd = contactMiddleCornerRadius,
            ),
            recipientSelectionContactRowShape(index = 0, totalCount = 3),
        )
        assertEquals(
            RoundedCornerShape(size = contactMiddleCornerRadius),
            recipientSelectionContactRowShape(index = 1, totalCount = 3),
        )
        assertEquals(
            RoundedCornerShape(
                topStart = contactMiddleCornerRadius,
                topEnd = contactMiddleCornerRadius,
                bottomStart = contactCornerRadius,
                bottomEnd = contactCornerRadius,
            ),
            recipientSelectionContactRowShape(index = 2, totalCount = 3),
        )
    }
}
