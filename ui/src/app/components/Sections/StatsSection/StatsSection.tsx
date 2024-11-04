import { DefaultSbomerApi } from '@app/api/DefaultSbomerApi';
import { SbomerStats } from '@app/types';
import { timestampToHumanReadable } from '@app/utils/Utils';
import {
  Card,
  CardBody,
  CardTitle,
  DescriptionList,
  DescriptionListDescription,
  DescriptionListGroup,
  DescriptionListTerm,
  HelperText,
  HelperTextItem,
  Skeleton,
} from '@patternfly/react-core';
import { InfoIcon } from '@patternfly/react-icons';
import axios from 'axios';
import React, { useCallback, useEffect, useState } from 'react';
import { ErrorSection } from '../ErrorSection/ErrorSection';
import { useAsyncRetry } from 'react-use';

const StatisticsContent = (props: { stats: SbomerStats }) => {
  return (
    <>
      <DescriptionList
        columnModifier={{
          default: '2Col',
        }}
      >
        <DescriptionListGroup>
          <DescriptionListTerm>Manifests</DescriptionListTerm>
          <DescriptionListDescription>{props.stats.resources.manifests.total}</DescriptionListDescription>
        </DescriptionListGroup>
        <DescriptionListGroup>
          <DescriptionListTerm>Generations</DescriptionListTerm>
          <DescriptionListDescription>
            {props.stats.resources.generations.total} ({props.stats.resources.generations.inProgress} in progress)
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>Version</DescriptionListTerm>
          <DescriptionListDescription>
            <a href={`https://github.com/project-ncl/sbomer/tree/${props.stats.version}`}>{props.stats.version}</a>
          </DescriptionListDescription>
        </DescriptionListGroup>

        <DescriptionListGroup>
          <DescriptionListTerm>Uptime</DescriptionListTerm>
          <DescriptionListDescription>{timestampToHumanReadable(props.stats.uptimeMillis)}</DescriptionListDescription>
        </DescriptionListGroup>

        {props.stats.messaging && (
          <>
            <DescriptionListGroup>
              <DescriptionListTerm>PNC Processed messages</DescriptionListTerm>
              <DescriptionListDescription>
                {props.stats.messaging.pncConsumer.processed} out of {props.stats.messaging.pncConsumer.received} (
                {props.stats.messaging.pncConsumer.received - props.stats.messaging.pncConsumer.processed} failed to
                process)
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Errata Processed messages</DescriptionListTerm>
              <DescriptionListDescription>
                {props.stats.messaging.errataConsumer.processed} out of {props.stats.messaging.errataConsumer.received}{' '}
                ({props.stats.messaging.errataConsumer.received - props.stats.messaging.errataConsumer.processed} failed
                to process)
              </DescriptionListDescription>
            </DescriptionListGroup>
            <DescriptionListGroup>
              <DescriptionListTerm>Produced messages</DescriptionListTerm>
              <DescriptionListDescription>
                {props.stats.messaging.producer.acked} ({props.stats.messaging.producer.nacked} failed to send)
              </DescriptionListDescription>
            </DescriptionListGroup>
          </>
        )}
      </DescriptionList>
    </>
  );
};

export const StatsSection = () => {
  const sbomerApi = DefaultSbomerApi.getInstance();

  const getStats = useCallback(async () => {
    try {
      return await sbomerApi.stats();
    } catch (e) {
      return Promise.reject(e);
    }
  }, []);

  const { loading, value, error } = useAsyncRetry(
    () =>
      getStats().then((data) => {
        return data;
      }),
    [],
  );

  if (error) {
    return <ErrorSection />;
  }

  return value ? (
    <Card isLarge>
      <CardTitle>Statistics</CardTitle>
      <CardBody>
        <HelperText>
          <HelperTextItem icon={<InfoIcon />}>
            Please note that statistics other than number of manifests and generation requests are currently misleading,
            because these do not take into account values from other nodes located across clusters. This will be
            addressed soon.
          </HelperTextItem>
        </HelperText>
        <br />
        <StatisticsContent stats={value!} />
      </CardBody>
    </Card>
  ) : (
    <Skeleton screenreaderText="Loading statistics..." />
  );
};
