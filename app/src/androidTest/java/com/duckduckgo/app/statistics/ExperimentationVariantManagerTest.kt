/*
 * Copyright (c) 2018 DuckDuckGo
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.duckduckgo.app.statistics

import android.os.Build
import android.support.test.filters.SdkSuppress
import com.duckduckgo.app.statistics.store.StatisticsDataStore
import com.nhaarman.mockito_kotlin.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExperimentationVariantManagerTest {

    private lateinit var testee: ExperimentationVariantManager

    private val mockStore: StatisticsDataStore = mock()
    private val mockRandomizer: IndexRandomizer = mock()
    private val activeVariants = mutableListOf<Variant>()

    @Before
    fun setup() {
        // mock randomizer always returns the first active variant
        whenever(mockRandomizer.random(any())).thenReturn(0)

        testee = ExperimentationVariantManager(mockStore, mockRandomizer)
    }

    @Test
    fun whenVariantAlreadyPersistedThenVariantReturned() {
        activeVariants.add(Variant("foo", 100.0))
        whenever(mockStore.variant).thenReturn("foo")

        assertEquals("foo", testee.getVariant(activeVariants).key)
    }

    @Test
    fun whenVariantAlreadyPersistedThenVariantAllocatorNeverInvoked() {
        activeVariants.add(Variant("foo", 100.0))
        whenever(mockStore.variant).thenReturn("foo")

        testee.getVariant(activeVariants)
        verify(mockRandomizer, never()).random(any())
    }

    @Test
    fun whenNoVariantsAvailableThenDefaultVariantHasEmptyStringForKey() {
        whenever(mockStore.variant).thenReturn("foo")

        val defaultVariant = testee.getVariant(activeVariants)
        assertEquals("", defaultVariant.key)
        assertTrue(defaultVariant.features.isEmpty())
    }

    @Test
    fun whenNoVariantsAvailableThenDefaultVariantHasNoExperimentalFeaturesEnabled() {
        whenever(mockStore.variant).thenReturn("foo")

        val defaultVariant = testee.getVariant(activeVariants)
        assertTrue(defaultVariant.features.isEmpty())
    }

    @Test
    fun whenVariantPersistedIsNotFoundInActiveVariantListThenRestoredToDefaultVariant() {
        activeVariants.add(Variant("foo", 100.0))
        whenever(mockStore.variant).thenReturn("bar")

        assertEquals(VariantManager.DEFAULT_VARIANT, testee.getVariant(activeVariants))
    }

    @Test
    fun whenVariantPersistedIsNotFoundInActiveVariantListThenNewVariantIsPersisted() {
        activeVariants.add(Variant("foo", 100.0))

        whenever(mockStore.variant).thenReturn("bar")
        testee.getVariant(activeVariants)

        verify(mockStore).variant = VariantManager.DEFAULT_VARIANT.key
    }


    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun whenNougatOrLaterAndNoVariantPersistedThenNewVariantAllocated() {
        activeVariants.add(Variant("foo", 100.0))

        testee.getVariant(activeVariants)

        verify(mockRandomizer).random(any())
    }

    @Test
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    fun whenNougatOrLaterAndNoVariantPersistedThenNewVariantKeyIsAllocatedAndPersisted() {
        activeVariants.add(Variant("foo", 100.0))

        testee.getVariant(activeVariants)

        verify(mockStore).variant = "foo"
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.M)
    fun whenMarshmallowOrEarlierAndNoVariantPersistedThenDefaultVariantAllocated() {
        activeVariants.add(Variant("foo", 100.0))

        assertEquals(VariantManager.DEFAULT_VARIANT, testee.getVariant(activeVariants))
    }

    @Test
    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.M)
    fun whenMarshmallowOrEarlierAndNoVariantPersistedThenDefaultVariantKeyIsAllocatedAndPersisted() {
        activeVariants.add(Variant("foo", 100.0))

        testee.getVariant(activeVariants)

        verify(mockStore).variant = VariantManager.DEFAULT_VARIANT.key
    }
}